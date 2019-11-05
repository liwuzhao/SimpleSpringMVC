package com.liwuzhao.demo.lwzcontroller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liwuzhao.demo.lwzservice.TestService;
import com.liwuzhao.lwzSpring.annotation.LWZAutowired;
import com.liwuzhao.lwzSpring.annotation.LWZController;
import com.liwuzhao.lwzSpring.annotation.LWZRequestMapping;

@LWZController
@LWZRequestMapping("test")
public class TestController {
	@LWZAutowired
	TestService testService;
	
	
	@LWZRequestMapping("lwz")
	public void testMethod(HttpServletRequest req, HttpServletResponse resp) {
        try {
            resp.getWriter().write(testService.test());
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
}
