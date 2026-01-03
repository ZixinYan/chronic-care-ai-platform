package com.zixin.utils.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import lombok.Getter;
import org.apache.http.HttpStatus;

import java.io.Serializable;

@Getter
public class Result<T> implements Serializable {
	private static final long serialVersionUID = 1L;

	private int code;
	private String msg;
	private T data;

	public Result() {
		this.code = 0;
		this.msg = "success";
	}

	public Result(int code, String msg, T data) {
		this.code = code;
		this.msg = msg;
		this.data = data;
	}

    /**
     * 成功返回结果
     * @return
     * @param <T>
     */
	public static <T> Result<T> success() {
		return new Result<>();
	}

	public static <T> Result<T> success(String msg) {
		return new Result<>(0, msg, null);
	}

	public static <T> Result<T> success(T data) {
		return new Result<>(0, "success", data);
	}

	public static <T> Result<T> success(String msg, T data) {
		return new Result<>(0, msg, data);
	}

    /**
     * 失败返回结果
     * @return
     * @param <T>
     */

	public static <T> Result<T> error() {
		return new Result<>(HttpStatus.SC_INTERNAL_SERVER_ERROR, "未知异常，请联系管理员", null);
	}

	public static <T> Result<T> error(int code, String msg) {
		return new Result<>(code, msg, null);
	}

    /**
     * 设置数据
     * @param data
     * @return
     */

	public Result<T> setData(T data) {
		this.data = data;
		return this;
	}

	public <V> V getData(TypeReference<V> typeReference) {
		String json = JSON.toJSONString(this.data);
		return JSON.parseObject(json, typeReference);
	}

}
