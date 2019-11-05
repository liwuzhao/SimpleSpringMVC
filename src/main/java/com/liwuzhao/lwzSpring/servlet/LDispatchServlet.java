package com.liwuzhao.lwzSpring.servlet;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.liwuzhao.lwzSpring.annotation.LWZAutowired;
import com.liwuzhao.lwzSpring.annotation.LWZController;
import com.liwuzhao.lwzSpring.annotation.LWZRequestMapping;
import com.liwuzhao.lwzSpring.annotation.LWZService;

public class LDispatchServlet extends HttpServlet {
	
	private Properties contextConfig = new Properties();
	private List<String> classNameList = new ArrayList<String>();
	
	/**
	 * IOC 容器
	 */
	Map<String, Object> iocMap = new HashMap<String, Object>();
	
	/**
	 * 方法映射
	 */
	Map<String, Method> handlerMapping = new HashMap<String, Method>();
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		this.doPost(req, resp);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			this.doDispatch(req, resp);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}
	
	/**
	 * 根据url进行方法分发
	 * @param req
	 * @param resp
	 * @throws InvocationTargetException 
	 * @throws IllegalArgumentException 
	 * @throws IllegalAccessException 
	 */
	private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		
		String url = req.getRequestURI();
		
		String contenxtPath = req.getContextPath();
		System.out.println("contextPath is : " + contenxtPath);
		url = url.replaceAll(contenxtPath, "").replaceAll("/+", "/");
        System.out.println("[INFO-7] request url-->" + url);
        
        /**
         * 从handlerMapping找找到对应的method对象
         */
        if (!handlerMapping.containsKey(url)) {
            try {
                resp.getWriter().write("404 NOT FOUND!!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
            }
		}
        
        Method method = handlerMapping.get(url);
        
        System.out.println("[INFO-7] method-->" + method);

        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
		
        System.out.println("[INFO-7] iocMap.get(beanName)->" + iocMap.get(beanName));
        
        // 调用方法
        method.invoke(iocMap.get(beanName), req, resp);
		
        System.out.println("[INFO-7] method.invoke put {" + iocMap.get(beanName) + "}.");
	}
	
	
	@Override
    public void init(ServletConfig servletConfig) throws ServletException {
		// 加载配置文件
        doLoadConfig(servletConfig.getInitParameter("contextConfigLocation"));
        
        // 扫描相关的类
        doScanner(contextConfig.getProperty("scan-package"));
        
        // 初始化 IOC 容器
        doInstance();
        
        // 依赖注入
        doAutowired();
        
        // 初始化 HandlerMapping
        initHandlerMapping();
	
	}
	
	/**
	 * 初始化 HandlerMapping
	 */
	private void initHandlerMapping() {
		
		if (iocMap.isEmpty()) {
			return;
		}
		
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Class<?> clazz = entry.getValue().getClass();
            
            // 是否有 controller 注解
			if (!clazz.isAnnotationPresent(LWZController.class)) {
				continue;
			}
			
            String baseUrl = "";
            
            // baseUrl
            if (clazz.isAnnotationPresent(LWZRequestMapping.class)) {
            	LWZRequestMapping lwzRequestMappingRequestMapping = clazz.getAnnotation(LWZRequestMapping.class);
                baseUrl = lwzRequestMappingRequestMapping.value();
            }
            
            // 遍历controller类的所有加了LWZRequestMapping注解方法，将这些方法都加载进 handlerMapping
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(LWZRequestMapping.class)) {
                    continue;
                }

                LWZRequestMapping xRequestMapping = method.getAnnotation(LWZRequestMapping.class);

                String url = ("/" + baseUrl + "/" + xRequestMapping.value()).replaceAll("/+", "/");

                handlerMapping.put(url, method);


            }
        }
	}

	/**
	 * 依赖注入
	 */
	private void doAutowired() {

		if (iocMap.isEmpty()) {
			return;
		}
		
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
        	// 获得这个bean的所有声明的字段，即包括public、private和proteced
        	Field[] fields = entry.getValue().getClass().getDeclaredFields();
        	for (Field field : fields) {
				if (!field.isAnnotationPresent(LWZAutowired.class)) {
					continue;
				}
				
				LWZAutowired lwzAutowired = field.getAnnotation(LWZAutowired.class);
				String beanName = lwzAutowired.value().trim();
				
				if ("".equals(beanName)) {
					beanName = field.getType().getName();
				}
				
				// 从IOC容器中将对应的bean设为这个类的成员变量
				field.setAccessible(true);
				try {
					field.set(entry.getValue(), iocMap.get(beanName));
				} catch (Exception e) {
					e.printStackTrace();
				}
				
			}
        }
	}

	/**
	 * 初始化IOC容器
	 */
	private void doInstance() {

		if (classNameList.isEmpty()) {
			return;
		}
		
		try {
			for (String className : classNameList) {
				
				Class<?> clazz = Class.forName(className);
				if (clazz.isAnnotationPresent(LWZController.class)) {
					String beanName = toLowerFirstCase(clazz.getSimpleName());
					Object instance = clazz.newInstance();
					
					iocMap.put(beanName, instance);
				} else if (clazz.isAnnotationPresent(LWZService.class)) {
					String beanName = toLowerFirstCase(clazz.getSimpleName());

					// 读取注解上的参数设为beanName
					LWZService lwzService = clazz.getAnnotation(LWZService.class);
					if (!"".equals(lwzService.value())) {
						beanName = lwzService.value();
					}
					
					Object instance = clazz.newInstance();
					iocMap.put(beanName, instance);
					
					// 找类的接口
					for (Class<?> i : clazz.getInterfaces()) {
                        if (iocMap.containsKey(i.getName())) {
                            throw new Exception("The Bean Name Is Exist.");
                        }

                        iocMap.put(i.getName(), instance);
					}
					
				}
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	/**
	 * 扫描所有的类加载紧classList
	 * @param property
	 */
	private void doScanner(String scanPackage) {
		URL resourcePath = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
		
		if (resourcePath == null) {
			return;
		}
		
		File classPath = new File(resourcePath.getFile());
		
		for (File file : classPath.listFiles()) {
			if (file.isDirectory()) {
				doScanner(scanPackage + "." + file.getName());
			} else {
				if (!file.getName().endsWith(".class")) {
					continue;
				}
                String className = (scanPackage + "." + file.getName()).replace(".class", "");
                
                classNameList.add(className);
                
			}
			
		}
		
	}

	/**
	 * 加载配置文件
	 * @param contextConfigLocation
	 */
	private void doLoadConfig(String contextConfigLocation) {
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
		
		try {
			// 保存
			contextConfig.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
		
	}
	
	

    /**
     * 获取类的首字母小写的名称
     *
     * @param className ClassName
     * @return java.lang.String
     */
    private String toLowerFirstCase(String className) {
        char[] charArray = className.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

	
}
