package com.zixin.accountprovider.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zixin.accountapi.api.AccountManagementAPI;
import com.zixin.accountapi.dto.*;
import com.zixin.accountapi.enums.Action;
import com.zixin.accountapi.enums.RoleCode;
import com.zixin.accountapi.po.*;
import com.zixin.accountapi.vo.DoctorVO;
import com.zixin.accountprovider.config.RoleConfig;
import com.zixin.accountprovider.mapper.*;
import com.zixin.accountprovider.utils.AccountUtils;
import com.zixin.utils.context.UserInfoManager;
import com.zixin.utils.exception.BusinessException;
import com.zixin.utils.exception.ToBCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Account Service Implementation
 */
@Slf4j
@DubboService
public class AccountServiceImpl extends ServiceImpl<UserMapper, User> implements AccountManagementAPI {

    private final UserRoleMapper userRoleMapper;
    private final RolePermissionMapper rolePermissionMapper;
    private final RoleConfig roleConfig;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final UserIdentityServiceImpl userIdentityService;

    // Password complexity regex
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$");

    // Phone number regex (simple example)
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^1[3-9]\\d{9}$");

    // ID card regex (simple example)
    private static final Pattern ID_CARD_PATTERN =
            Pattern.compile("^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx]$");

    public AccountServiceImpl(UserRoleMapper userRoleMapper,
                              RolePermissionMapper rolePermissionMapper,
                              RoleConfig roleConfig,
                              BCryptPasswordEncoder bCryptPasswordEncoder, DoctorMapper doctorMapper, PatientMapper patientMapper, UserIdentityServiceImpl userIdentityService) {
        this.userRoleMapper = userRoleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.roleConfig = roleConfig;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.doctorMapper = doctorMapper;
        this.patientMapper = patientMapper;
        this.userIdentityService = userIdentityService;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String loginAccount = loginRequest.getLoginAccount();
        String password = loginRequest.getPassword();
        LoginResponse loginResponse = new LoginResponse();

        // 1. Validate request parameters
        if (!StringUtils.hasText(loginAccount) || !StringUtils.hasText(password)) {
            log.warn("Login failed - empty account or password");
            loginResponse.setCode(ToBCodeEnum.FAIL);
            loginResponse.setMessage("Login account or password cannot be empty");
            return loginResponse;
        }

        User user;
        try {
            // 2. Find account by username/phone hash/id card hash
            String accountHash = generateHash(loginAccount);

            user = this.baseMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, loginAccount)
                    .or()
                    .eq(User::getPhoneHash, accountHash)
                    .or()
                    .eq(User::getIdCardHash, accountHash)
                    .last("LIMIT 1"));

        } catch (Exception e) {
            log.error("Failed to query account by loginAccount: {}, error: {}", loginAccount, e.getMessage());
            loginResponse.setCode(ToBCodeEnum.FAIL);
            loginResponse.setMessage("System error, please try again later");
            return loginResponse;
        }

        if (user == null) {
            log.info("Login failed - account not found: {}", loginAccount);
            loginResponse.setCode(ToBCodeEnum.FAIL);
            loginResponse.setMessage("Account or password incorrect");
            return loginResponse;
        }

        // 3. Verify password using BCrypt
        if (!bCryptPasswordEncoder.matches(password, user.getPassword())) {
            log.info("Login failed - wrong password for account: {}, password:{}, rawPassword:{}", loginAccount, password, user.getPassword());

            // Record login failure (optional)
            recordLoginFailure(user.getUserId());

            loginResponse.setCode(ToBCodeEnum.FAIL);
            loginResponse.setMessage("Account or password incorrect");
            return loginResponse;
        }

        // 4. Clear failure records on successful login
        clearLoginFailures(user.getUserId());

        // 5. Get user roles and permissions
        List<Integer> roleCodes = getUserRoleCodes(user.getUserId());
        Set<String> permissions = getUserPermissions(roleCodes);

        // 6. Build response
        LoginResponse.LoginUserDTO userDTO = buildLoginUserDTO(user, roleCodes, permissions);

        log.info("Login user data - userId: {}, phone: {}, email: {}", user.getUserId(), user.getPhone(), user.getEmail());

        loginResponse.setData(userDTO);
        loginResponse.setCode(ToBCodeEnum.SUCCESS);
        loginResponse.setMessage("Login successful");

        log.info("Login successful - account: {}, userId: {}", loginAccount, user.getUserId());
        return loginResponse;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterResponse register(RegisterRequest registerRequest) {
        RegisterResponse registerResponse = new RegisterResponse();

        try {
            // 1. Validate request parameters
            validateRegisterRequest(registerRequest);

            // 2. Check account uniqueness
            checkAccountUniqueness(registerRequest);

            // 3. Build user object
            User user = buildUserFromRequest(registerRequest);
            log.info("Register account: {}", user);
            // 4. Insert user
            this.baseMapper.insert(user);

            // 5. Assign roles (batch insert)
            assignRolesToUser(user.getUserId(), registerRequest.getRoleCodes());

            // 6. set Doctor information
            Long userId = this.baseMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getUsername, registerRequest.getUsername())
                    .last("LIMIT 1"))
                    .getUserId();
            if(registerRequest.getRoleCodes().contains(RoleCode.DOCTOR.getCode())){
                registerRequest.getDoctor().setUserId(userId);
                registerRequest.getDoctor().setUsername(user.getUsername());
                doctorMapper.insert(BeanUtil.copyProperties(registerRequest.getDoctor(), Doctor.class));
                log.info("Doctor info inserted for userId: {}, doctor information: {}", user.getUserId(), registerRequest.getDoctor());
            }
            // 7. set Patient information
            if (registerRequest.getRoleCodes().contains(RoleCode.PATIENT.getCode())){
                registerRequest.getPatient().setUserId(userId);
                registerRequest.getPatient().setUsername(user.getUsername());
                if (checkPatientInfo(registerRequest.getPatient())){
                    throw new BusinessException("患者信息不合法");
                }
                patientMapper.insert(BeanUtil.copyProperties(registerRequest.getPatient(), Patient.class));
                log.info("Patient info inserted for userId: {}, patient information: {}", user.getUserId(), registerRequest.getPatient());
            }

            log.info("User registered successfully - userId: {}, username: {}",
                    user.getUserId(), user.getUsername());

            registerResponse.setCode(ToBCodeEnum.SUCCESS);
            registerResponse.setMessage("Registration successful");

        } catch (BusinessException e) {
            log.warn("Registration failed: {}", e.getMessage());
            registerResponse.setCode(ToBCodeEnum.FAIL);
            registerResponse.setMessage(e.getMessage());
            throw e; // Rollback transaction for business exceptions
        } catch (DuplicateKeyException e) {
            log.error("Registration failed - duplicate account: {}", registerRequest.getUsername());
            registerResponse.setCode(ToBCodeEnum.FAIL);
            registerResponse.setMessage("Username, phone number or ID card already exists");
            throw e;
        } catch (Exception e) {
            log.error("Registration failed - system error", e);
            registerResponse.setCode(ToBCodeEnum.FAIL);
            registerResponse.setMessage("System error, please try again later");
            throw e;
        } finally {
            return registerResponse;
        }
    }


    @Override
    public GetUserInfoResponse getUserInfo(GetUserInfoRequest request) {
        GetUserInfoResponse response = new GetUserInfoResponse();

        try {
            List<Long> userIds = request.getUserIds();
            if (userIds == null || userIds.isEmpty()) {
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setUsers(Collections.emptyList());
                return response;
            }

            // Batch query users (safe way)
            List<User> users = lambdaQuery()
                    .in(User::getUserId, userIds)
                    .list();

            // Convert to Map for maintaining order
            Map<Long, User> userMap = users.stream()
                    .collect(Collectors.toMap(User::getUserId, u -> u));

            List<GetUserInfoResponse.UserInfoDTO> userInfoDTOList = userIds.stream()
                    .map(userMap::get)
                    .filter(Objects::nonNull)
                    .map(user -> {
                        GetUserInfoResponse.UserInfoDTO dto = BeanUtil.copyProperties(user, GetUserInfoResponse.UserInfoDTO.class);
                        dto.setAvatarUrl(user.getAvatar());
                        List<Integer> roleCodes = getUserRoleCodes(user.getUserId());
                        List<String> roleNames = convertRoleCodesToNames(roleCodes);
                        dto.setRoles(roleNames);
                        Set<String> permissions = getUserPermissions(roleCodes);
                        dto.setPermissions(new ArrayList<>(permissions));
                        log.info("User {} roles: {}, permissions: {}", user.getUserId(), roleNames, permissions);
                        return dto;
                    })
                    .collect(Collectors.toList());

            log.info("userInfoDTOList size: {}, first user roles: {}", 
                    userInfoDTOList.size(), 
                    userInfoDTOList.isEmpty() ? "N/A" : userInfoDTOList.get(0).getRoles());

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setUsers(userInfoDTOList);

            log.debug("Retrieved info for {} users", userInfoDTOList.size());

        } catch (Exception e) {
            log.error("Failed to get user info", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("Failed to get user info: " + e.getMessage());
        }

        return response;
    }

    @Override
    public UpdateUserInfoResponse updateUserInfo(UpdateUserInfoRequest request) {
        UpdateUserInfoResponse response = new UpdateUserInfoResponse();

        try {
            Map<String, Object> updateData = request.getUpdateData();
            Long userId;
            try {
                userId = UserInfoManager.getUserId();
            } catch (NumberFormatException e) {
                log.warn("Update failed - invalid userid,{}", UserInfoManager.getUserId());
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Invalid account_id format");
                return response;
            }

            // Validate update data
            if (!AccountUtils.validateUpdateData(updateData)) {
                log.warn("Update failed - illegal update data: {}", updateData);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Illegal update data");
                return response;
            }

            // Check if user exists
            User user = this.baseMapper.selectById(userId);
            if (user == null) {
                log.warn("Update failed - user not found: {}", userId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("User not found");
                return response;
            }

            // check if update password
            if(updateData.containsKey("password")){
                if(!user.getPhone().equals(updateData.get("phone"))){
                    log.error("Attempt to update password without same phone number: {}", userId);
                    response.setCode(ToBCodeEnum.FAIL);
                    response.setMessage("Please user same phone number to update password");
                    return response;
                }
            }

            // Build update condition
            UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
            updateWrapper.eq("user_id", userId);

            updateData.forEach((key, value) -> {
                if (value != null && !"user_id".equals(key)) {
                    updateWrapper.set(key, value);

                    // Handle special fields
                    if ("phone".equals(key)) {
                        String phoneHash = DigestUtils.sha256Hex(value.toString());
                        updateWrapper.set("phone_hash", phoneHash);
                    } else if ("id_card".equals(key)) {
                        String idCardHash = DigestUtils.sha256Hex(value.toString());
                        updateWrapper.set("id_card_hash", idCardHash);
                    }
                }
            });

            updateWrapper.set("update_time", System.currentTimeMillis());

            // Execute update
            boolean success = update(updateWrapper);
            if (!success) {
                log.warn("Update failed for user: {}", userId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Update failed");
                return response;
            }

            log.info("User info updated successfully - userId: {}", userId);
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("Update successful");

        } catch (Exception e) {
            log.error("Failed to update user info", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("Update failed: " + e.getMessage());
        }

        return response;
    }

    @Override
    public UpdatePasswordResponse updatePassword(UpdatePasswordRequest request) {
        Long userId = request.getUserId();
        String oldPassword = request.getOldPassword();
        String newPassword = request.getNewPassword();

        UpdatePasswordResponse response = new UpdatePasswordResponse();

        try {
            // Validate input
            if (userId == null || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
                log.warn("Password update failed - missing required parameters");
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("User ID, old password and new password are required");
                return response;
            }

            // Validate new password complexity
            if (!isValidPassword(newPassword)) {
                log.warn("Password update failed - new password does not meet complexity requirements");
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Password must be 8-20 characters and contain at least one uppercase letter, one lowercase letter, one number and one special character");
                return response;
            }

            // Check if old password equals new password
            if (oldPassword.equals(newPassword)) {
                log.warn("Password update failed - new password cannot be the same as old password");
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("New password cannot be the same as old password");
                return response;
            }

            // 1. Find user
            User user = this.baseMapper.selectById(userId);
            if (user == null) {
                log.warn("Password update failed - user not found: {}", userId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("User not found");
                return response;
            }

            // 2. Verify old password using BCrypt
            if (!bCryptPasswordEncoder.matches(oldPassword, user.getPassword())) {
                log.warn("Password update failed - wrong old password for user: {}", userId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Old password is incorrect");
                return response;
            }

            // 3. Encrypt and update new password
            String encodedNewPassword = bCryptPasswordEncoder.encode(newPassword);
            user.setPassword(encodedNewPassword);
            this.baseMapper.updateById(user);

            log.info("Password updated successfully for user: {}", userId);
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("Password updated successfully");

        } catch (Exception e) {
            log.error("Failed to update password for user: {}", userId, e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("Password update failed: " + e.getMessage());
        }

        return response;

    }

    @Override
    public DeleteUserResponse deleteUser(DeleteUserRequest deleteUserRequest) {
        DeleteUserResponse response = new DeleteUserResponse();

        try {
            Long[] userIds = deleteUserRequest.getUserId();
            if (userIds == null || userIds.length == 0) {
                log.warn("Delete failed - no user IDs provided");
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("User IDs are required");
                return response;
            }

            boolean success = removeByIds(Arrays.asList(userIds));

            if (success) {
                log.info("Users deleted successfully: {}", Arrays.toString(userIds));
                response.setCode(ToBCodeEnum.SUCCESS);
                response.setMessage("Users deleted successfully");
            } else {
                log.warn("Failed to delete users: {}", Arrays.toString(userIds));
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Failed to delete users");
            }

        } catch (Exception e) {
            log.error("Failed to delete users", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("Delete failed: " + e.getMessage());
        }

        return response;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UpdateUserRolesResponse updateUserRoles(UpdateUserRolesRequest request) {
        UpdateUserRolesResponse response = new UpdateUserRolesResponse();

        try {
            Long userId = request.getUserId();
            List<Integer> newRoleCodes = request.getRoleCodes();

            if (userId == null) {
                log.warn("Update roles failed - account ID is null");
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Account ID is required");
                return response;
            }

            if (newRoleCodes == null || newRoleCodes.isEmpty()) {
                log.warn("Update roles failed - role list is empty for account: {}", userId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Role list cannot be empty");
                return response;
            }

            // Verify account exists
            User user = this.baseMapper.selectById(userId);
            if (user == null) {
                log.warn("Update roles failed - account not found: {}", userId);
                response.setCode(ToBCodeEnum.FAIL);
                response.setMessage("Account not found");
                return response;
            }

            // Delete all existing roles
            userRoleMapper.deleteByUserId(userId);

            // Insert new roles (batch insert)
            if (!newRoleCodes.isEmpty()) {
                List<UserRole> userRoles = newRoleCodes.stream()
                        .map(roleCode -> {
                            UserRole userRole = new UserRole();
                            userRole.setUserId(userId);
                            userRole.setRoleCode(roleCode);
                            userRole.setCreateTime(System.currentTimeMillis());
                            return userRole;
                        })
                        .collect(Collectors.toList());

                userRoleMapper.batchInsert(userRoles);
            }

            log.info("User roles updated successfully - userId: {}, new roles: {}",
                    userId, newRoleCodes);

            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("Roles updated successfully");
            response.setRoleCodes(newRoleCodes);

        } catch (Exception e) {
            log.error("Failed to update user roles for userId: {}", request.getUserId(), e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("Failed to update roles: " + e.getMessage());
        }

        return response;
    }

    /**
     * Generate hash with salt
     */
    private String generateHash(String input) {
        // Add salt for better security
        String salt = "zixin-salt"; // Should be read from configuration
        return DigestUtils.sha256Hex(salt + input);
    }

    /**
     * Get user role codes
     */
    private List<Integer> getUserRoleCodes(Long userId) {
        return userRoleMapper.selectRoleCodesByUserId(userId);
    }

    /**
     * Convert role codes to role names
     */
    private List<String> convertRoleCodesToNames(List<Integer> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return new ArrayList<>();
        }
        return roleCodes.stream()
                .map(code -> {
                    try {
                        return RoleCode.fromCode(code).name();
                    } catch (IllegalArgumentException e) {
                        log.warn("Unknown role code: {}", code);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Get user permissions from role codes
     */
    private Set<String> getUserPermissions(List<Integer> roleCodes) {
        if (roleCodes.isEmpty()) {
            return Collections.emptySet();
        }

        List<RolePermission> rolePermissions = rolePermissionMapper.selectByRoleCodes(roleCodes);
        log.info("Fetched {} permissions for roles: {}", rolePermissions, roleCodes);
        return rolePermissions.stream()
                .map(this::formatPermission)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Format permission string
     */
    private String formatPermission(RolePermission rp) {
        try {
            String roleName = RoleCode.fromCode(rp.getRoleCode()).name();
            String actionName = Action.fromCode(rp.getActionCode()).name();
            return roleName + ":" + actionName;
        } catch (IllegalArgumentException e) {
            log.warn("Invalid role or action code: roleCode={}, actionCode={}",
                    rp.getRoleCode(), rp.getActionCode());
            return null;
        }
    }

    /**
     * Build login user DTO
     */
    private LoginResponse.LoginUserDTO buildLoginUserDTO(User user, List<Integer> roleCodes, Set<String> permissions) {
        List<String> roleNames = convertRoleCodesToNames(roleCodes);
        return new LoginResponse.LoginUserDTO(
                user.getUserId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getEmail(),
                user.getGender(),
                user.getAvatar(),
                user.getAddress(),
                user.getBirthday(),
                user.getIdCard(),
                roleNames,
                permissions,
                user.getExt()
        );
    }

    /**
     * Validate password complexity
     */
    private boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validate phone number
     */
    private boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validate ID card
     */
    private boolean isValidIdCard(String idCard) {
        return idCard != null && ID_CARD_PATTERN.matcher(idCard).matches();
    }

    /**
     * Validate registration request
     */
    private void validateRegisterRequest(RegisterRequest request) {
        if (!StringUtils.hasText(request.getUsername())) {
            throw new BusinessException("Username is required");
        }

        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException("Password is required");
        }

        if (!isValidPassword(request.getPassword())) {
            throw new BusinessException("Password must be 8-20 characters and contain at least one uppercase letter, one lowercase letter, one number and one special character");
        }

        if (StringUtils.hasText(request.getPhone()) && !isValidPhone(request.getPhone())) {
            throw new BusinessException("Invalid phone number format");
        }

        if (StringUtils.hasText(request.getIdCard()) && !isValidIdCard(request.getIdCard())) {
            throw new BusinessException("Invalid ID card format");
        }
    }

    /**
     * Check account uniqueness
     */
    private void checkAccountUniqueness(RegisterRequest request) {
        // Check username
        Long usernameCount = lambdaQuery()
                .eq(User::getUsername, request.getUsername())
                .count();
        if (usernameCount > 0) {
            throw new BusinessException("Username already exists");
        }

        // Check phone
        if (StringUtils.hasText(request.getPhone())) {
            String phoneHash = generateHash(request.getPhone());
            Long phoneCount = lambdaQuery()
                    .eq(User::getPhoneHash, phoneHash)
                    .count();
            if (phoneCount > 0) {
                throw new BusinessException("Phone number already registered");
            }
        }

        // Check ID card
        if (StringUtils.hasText(request.getIdCard())) {
            String idCardHash = generateHash(request.getIdCard());
            Long idCardCount = lambdaQuery()
                    .eq(User::getIdCardHash, idCardHash)
                    .count();
            if (idCardCount > 0) {
                throw new BusinessException("ID card already registered");
            }
        }
    }

    /**
     * Build user from registration request
     */
    private User buildUserFromRequest(RegisterRequest request) {
        User user = new User();

        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setPhone(request.getPhone());
        user.setAddress(request.getAddress());
        user.setBirthday(request.getBirthday());
        user.setGender(request.getGender());
        user.setIdCard(request.getIdCard());

        // Generate phone hash
        if (StringUtils.hasText(request.getPhone())) {
            user.setPhoneHash(generateHash(request.getPhone()));
        }

        // Generate ID card hash
        if (StringUtils.hasText(request.getIdCard())) {
            user.setIdCardHash(generateHash(request.getIdCard()));
        }

        // Encrypt password
        user.setPassword(bCryptPasswordEncoder.encode(request.getPassword()));
        return user;
    }

    /**
     * Assign roles to user (batch insert)
     */
    private void assignRolesToUser(Long userId, List<Integer> roleCodes) {
        // Determine final role list
        List<Integer> finalRoleCodes = roleCodes;
        if (finalRoleCodes == null || finalRoleCodes.isEmpty()) {
            finalRoleCodes = roleConfig.getDefaultRoles();
            log.info("User {} assigned default roles: {}", userId, finalRoleCodes);
        }

        if (finalRoleCodes.isEmpty()) {
            return;
        }

        // Batch insert
        List<UserRole> userRoles = finalRoleCodes.stream()
                .map(roleCode -> {
                    UserRole userRole = new UserRole();
                    userRole.setUserId(userId);
                    userRole.setRoleCode(roleCode);
                    userRole.setCreateTime(System.currentTimeMillis());
                    return userRole;
                })
                .collect(Collectors.toList());

        userRoleMapper.batchInsert(userRoles);
        log.info("Assigned {} roles to user {}", userRoles.size(), userId);
    }

    /**
     * Record login failure (optional)
     */
    private void recordLoginFailure(Long userId) {
        // Implementation depends on your requirements
        // Could increment a counter in Redis
    }

    /**
     * Clear login failure records (optional)
     */
    private void clearLoginFailures(Long userId) {
        // Implementation depends on your requirements
        // Could delete failure records from Redis
    }

    /**
     * Business Exception class
     */
    private static class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }

    /**
     * Check patient information validity
     * @param patient
     * @return
     */
    private boolean checkPatientInfo(Patient patient) {
        if (patient == null) {
            return true;
        }
        Long doctorId = patient.getAttendingDoctorId();
        if (doctorId == null) {
            return true;
        }
        // 1. Check if doctor exists
        DoctorVO doctor = userIdentityService.getDoctorInfoByUserId(doctorId).getDoctor();
        if (doctor == null) {
            return true;
        }
        patient.setAttendingDoctorName(doctor.getUsername());
        return false;
    }

    @Override
    public GetUsersListResponse getUsersList(GetUsersListRequest request) {
        GetUsersListResponse response = new GetUsersListResponse();
        
        try {
            Integer pageNum = request.getPageNum() != null ? request.getPageNum() : 1;
            Integer pageSize = request.getPageSize() != null ? request.getPageSize() : 10;
            String keyword = request.getKeyword();
            Integer roleCode = request.getRoleCode();
            
            LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
            
            if (StrUtil.isNotBlank(keyword)) {
                queryWrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
                    .or()
                    .like(User::getPhone, keyword)
                    .or()
                    .like(User::getEmail, keyword)
                );
            }
            
            queryWrapper.orderByDesc(User::getCreateTime);
            
            long total = this.baseMapper.selectCount(queryWrapper);
            
            int offset = (pageNum - 1) * pageSize;
            queryWrapper.last("LIMIT " + offset + ", " + pageSize);
            
            List<User> users = this.baseMapper.selectList(queryWrapper);
            
            List<GetUsersListResponse.UserItemDTO> userItems = users.stream()
                .map(user -> {
                    GetUsersListResponse.UserItemDTO dto = new GetUsersListResponse.UserItemDTO();
                    dto.setUserId(user.getUserId());
                    dto.setUsername(user.getUsername());
                    dto.setNickname(user.getNickname());
                    dto.setGender(user.getGender());
                    dto.setPhone(user.getPhone());
                    dto.setEmail(user.getEmail());
                    dto.setAvatarUrl(user.getAvatar());
                    dto.setAddress(user.getAddress());
                    dto.setBirthday(user.getBirthday());
                    dto.setCreateTime(user.getCreateTime());
                    dto.setUpdateTime(user.getUpdateTime());
                    
                    List<Integer> roleCodes = getUserRoleCodes(user.getUserId());
                    List<String> roleNames = convertRoleCodesToNames(roleCodes);
                    dto.setRoles(roleNames);
                    dto.setRoleCodes(roleCodes);
                    
                    if (roleCode != null && !roleCodes.contains(roleCode)) {
                        return null;
                    }
                    
                    return dto;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            
            if (roleCode != null) {
                total = userItems.size();
            }
            
            response.setUsers(userItems);
            response.setTotal(total);
            response.setPageNum(pageNum);
            response.setPageSize(pageSize);
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("查询成功");
            
            log.info("Get users list success, total: {}, pageNum: {}, pageSize: {}", total, pageNum, pageSize);
            
        } catch (Exception e) {
            log.error("Failed to get users list", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("查询用户列表失败: " + e.getMessage());
        }
        
        return response;
    }

    @Override
    public SystemStatsResponse getSystemStats() {
        SystemStatsResponse response = new SystemStatsResponse();
        
        try {
            long totalUsers = this.baseMapper.selectCount(null);
            
            List<UserRole> allUserRoles = userRoleMapper.selectList(null);
            Map<Integer, Long> roleCountMap = allUserRoles.stream()
                .collect(Collectors.groupingBy(UserRole::getRoleCode, Collectors.counting()));
            
            response.setTotalUsers(totalUsers);
            response.setTotalDoctors(roleCountMap.getOrDefault(RoleCode.DOCTOR.getCode(), 0L));
            response.setTotalPatients(roleCountMap.getOrDefault(RoleCode.PATIENT.getCode(), 0L));
            response.setTotalAdmins(roleCountMap.getOrDefault(RoleCode.ADMIN.getCode(), 0L));
            
            long todayStart = java.time.LocalDate.now()
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
            
            long todayNewUsers = this.baseMapper.selectCount(
                new LambdaQueryWrapper<User>()
                    .ge(User::getCreateTime, todayStart)
            );
            response.setTodayNewUsers(todayNewUsers);
            
            response.setActiveUsersToday(todayNewUsers);
            
            response.setCode(ToBCodeEnum.SUCCESS);
            response.setMessage("获取统计数据成功");
            
            log.info("Get system stats success - totalUsers: {}, doctors: {}, patients: {}, admins: {}", 
                totalUsers, response.getTotalDoctors(), response.getTotalPatients(), response.getTotalAdmins());
            
        } catch (Exception e) {
            log.error("Failed to get system stats", e);
            response.setCode(ToBCodeEnum.FAIL);
            response.setMessage("获取统计数据失败: " + e.getMessage());
        }
        
        return response;
    }

}