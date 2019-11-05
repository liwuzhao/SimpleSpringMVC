package com.liwuzhao.demo.lwzservice;

import com.liwuzhao.lwzSpring.annotation.LWZService;

@LWZService
public class TestServiceImpl implements TestService{

	public String test() {
		return "lwztttt";
	}

}
