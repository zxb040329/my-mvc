package com.example.servlet;

import com.alibaba.fastjson.JSONObject;
import com.example.commons.AnnotationClassNames;
import com.example.commons.HttpInfo;
import com.example.commons.MimeType;
import com.example.definition.MethodDefinition;
import io.github.classgraph.*;
import org.springframework.beans.BeanUtils;
import org.springframework.util.ClassUtils;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *  springmvc的执行流程：一定先从启动谈起。
 *
 */
public class InitServlet extends HttpServlet {

    private Map<String, MethodDefinition> map = new HashMap<>();

    private String prefix;  //前缀
    private String suffix; //后缀

    private ClassLoader classLoader;

    @Override
    public void init(ServletConfig config) throws ServletException {
        /**
         * <init-param>
         *       <param-name>basePackage</param-name>
         *       <param-value>com.example</param-value>
         * </init-param>
         */
        String basePackage = config.getInitParameter("basePackage");
        this.prefix =  config.getInitParameter("prefix");
        this.suffix = config.getInitParameter("suffix");

        // 扫描 com.example下的所有的类
        ScanResult result = new ClassGraph().enableAllInfo().whitelistPackages(basePackage).scan();

        // 该方法从扫描的结果中获取所有的类中包含 @MyController 这个注解的类
        ClassInfoList classInfoList = result.getClassesWithAnnotation("com.example.annotation.MyController");

        for(ClassInfo classInfo : classInfoList) {
            //获取到当前类的全类名  com.example.controller.TestController  com.example.controller.UserController
            String className = classInfo.getName();

            // 实例头顶上有 @MyController的类的实例
            Object obj = createInstances(new Class<?>[]{}, getClassLoader(), new Object[]{}, className);

            //获取类的@MyRequestMapping的value值, 因为请求的完整的路径是 是类上 uri + 方法的 uri
            String value = valueOfTypeRequestMapping(classInfo);

            // 获取类中所有的方法
            MethodInfoList methodInfos = classInfo.getMethodInfo();

            for(MethodInfo info : methodInfos) {
                proccessMethods(info, value, obj);
            }
        }
    }

    /**
     * @param info
     * @param value   是类上的 @MyRequestMapping 的value值
     * @param obj    是当前方法所属的对象
     */
    private void proccessMethods(MethodInfo info, String value, Object obj) {
        //获取方法头顶上的 @MyRequestMapping, 如果不存在，是不应该被纳入到 mvc 框架中的
        AnnotationInfo methodAnnotationInfo = info.getAnnotationInfo(AnnotationClassNames.MY_REQUEST_MAPPING);

        if(null != methodAnnotationInfo) {
            // 获取方法上 @MyRequestMapping 的信息
            AnnotationParameterValueList values = methodAnnotationInfo.getParameterValues();

            // 获取方法头顶上的 @MyRequestMapping(value="/all") 的value值
            String methodRequestMappingPathValue = (String)values.getValue("value");

            //获取到 Java反射对应的方法
            Method method = info.loadClassAndGetMethod();

            // 拼接完整的请求路径 /user + /getAll
            String fullUri = (value == null ? "" : value) +  methodRequestMappingPathValue;

            // 如果含有相同的uri, 那么表示重复了，要抛出异常
            if(map.containsKey(fullUri)) {
                throw new RuntimeException("路径重复. " + fullUri);
            }

            MethodDefinition methodDefinition = createMethodDefinition(method, info, obj);

            map.put(fullUri, methodDefinition);
        }
    }

    /**
     * @param method
     * @param info
     * @param obj   方法对应的对象
     * @return
     */
    public MethodDefinition createMethodDefinition(Method method, MethodInfo info, Object obj) {
        MethodDefinition methodDefinition = new MethodDefinition();
        methodDefinition.setObj(obj);
        methodDefinition.setMethod(method);
        // 获取方法上的 @MyResponseBody 注解
        AnnotationInfo methodResponseAnnotationInfo = info.getAnnotationInfo(AnnotationClassNames.MY_RESPONSE_BODY);
        //如果没有这个注解，表示不转json
        if(null != methodResponseAnnotationInfo) {
            methodDefinition.setJson(true);
        }

        methodParameterProccess(method, methodDefinition);

        return methodDefinition;
    }

    /**
     * 处理方法的参数部分
     * @param method
     * @param methodDefinition
     */
    public void methodParameterProccess(Method method, MethodDefinition methodDefinition) {
        Parameter[] parameters = method.getParameters();
        if(null != parameters && parameters.length > 0) {
            for(Parameter p : parameters) {
                String name = p.getName();  //参数名
                Class<?> paramterType = p.getType();  // 参数的类型
                methodDefinition.getParametersInfo().put(name, paramterType);
            }
        }
    }

    /**
     * 返回头顶上有 @MyRequestMapping 的类的对应的改注解的 value值
     * @return
     */
    private String valueOfTypeRequestMapping(ClassInfo classInfo) {
        // 获取头顶上有 @MyController 的类，的@MyRequestMapping这个注解信息
        AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(AnnotationClassNames.MY_REQUEST_MAPPING);

        String value = null;  //类上的  @MyRequestMapping 的值
        if(null != annotationInfo) {
            //System.out.println("头顶上包含了 @MyRequestMapping 的类是：" + classInfo.getName());

            //获取 @MyRequestMapping 的所有的属性, 因为注解的属性可能有多个
            AnnotationParameterValueList values = annotationInfo.getParameterValues();
            value = (String)values.getValue("value");
        }
        return value;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String uri = req.getRequestURI(); //users   /mvc/user/get
        String contextPath = req.getContextPath(); // 获取上下 /mvc/文路径, 说白就是获取到 /mvc

        String resourceUri = uri.substring(contextPath.length()); //获取到资源路径，抛开contextPath的东西

        // 获取资源地址对应的方法的信息
        MethodDefinition methodDefinition = map.get(resourceUri);
        //如果为空，表示没有信息
        if(null == methodDefinition) {
            accessError(resp, HttpInfo.NO_RESOURCE, HttpInfo.NO_RESOURCE_TIPS); //如果转换错误，就是404
            return;
        }

        // 方法的参数信息  {name:String, age:Integer}
        Map<String, Class<?>> parameterInfos = methodDefinition.getParametersInfo();

        Map<String, Object> parameterValuesMap = new HashMap<>();

        Object[] parameterValues = new Object[parameterInfos.size()];

        /**
         * 之所以判断，是因为方法的定义上可能没有参数，但是用户却传了参数，而这种调用方式是正确。
         * 但是我们不用去处理
         */
        if(parameterInfos.size() > 0) {
            // 获取所有的请求的参数的名字
            Enumeration<String> enumeration = req.getParameterNames();
            while(enumeration.hasMoreElements()) {
                // enumeration.nextElement()取出某个参数
                String paramName = enumeration.nextElement();
                // 获取参数
                String value = req.getParameter(paramName);

                // 获取参数的实际类型
                Class<?> clazz = parameterInfos.get(paramName);

                try {
                    Object obj = convertType(value, clazz);
                    parameterValuesMap.put(paramName, obj);
                } catch (Exception e) {
                    accessError(resp, HttpInfo.PARAMETER_ERROR, HttpInfo.PARAMETER_ERROR_TIPS); //如果转换错误，就是401
                }
            }

            int[] arr = new int[1];  //定义一个长度为1数组

            parameterInfos.forEach((k, v) -> {
                parameterValues[arr[0]++] =  parameterValuesMap.get(k);
            });
        }

        Object obj = methodDefinition.getObj(); //方法对应的对象
        Method method = methodDefinition.getMethod();
        boolean isJson = methodDefinition.getJson(); //是否返回json

        try {
            Object result = method.invoke(obj, parameterValues); //调用具体的方法
            if(isJson) {
                toJson(resp, result);  //返回json
            }else {
                //请求转发
                dispatch(req, resp, String.valueOf(result));
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * 访问错误处理
     * @param resp
     * @param code
     * @param tips
     * @throws IOException
     */
    private void accessError(HttpServletResponse resp, int code, String tips) throws IOException {
        resp.setStatus(code);  //401表示参数类型错误
        resp.setContentType(MimeType.HTML_MIME);
        String html = "<html><body><h3>" + tips + "</h3></body></html>";
        PrintWriter writer = resp.getWriter();
        writer.write(html);
        writer.flush();
        writer.close();
    }

    // 返回json数据
    public void toJson(HttpServletResponse resp, Object obj) throws IOException {
        resp.setContentType("application/json;charset=utf-8");
        PrintWriter writer = resp.getWriter();
        writer.write(JSONObject.toJSONString(obj));
        writer.flush();
        writer.close();
    }
    /**
     *  prefix
     *  suffix
     */
    public void dispatch(HttpServletRequest req, HttpServletResponse resp, String path) throws IOException, ServletException {
        String disptchPath = this.prefix + path + "." + this.suffix;
        System.out.println(disptchPath);
        req.getRequestDispatcher(disptchPath).forward(req, resp);
    }

    /**
     * 根据全类名的方式创建类的实例
     * @param parameterTypes       是构造方法的的类型数组
     * @param classLoader          类加载器
     * @param args                 构造方法的实际值
     * @param name                类的全类名
     * @param <T>
     * @return
     */
    private <T> T createInstances(Class<?>[] parameterTypes, ClassLoader classLoader, Object[] args, String name) {
        T instance = null;
        try {
            // Class.forName("com.mysql.jdbc.Driver")
            Class<?> instanceClass = ClassUtils.forName(name, classLoader);
            // 获取构造方法
            Constructor<?> constructor = instanceClass.getDeclaredConstructor(parameterTypes);
            // 实例化对象
            instance = (T) BeanUtils.instantiateClass(constructor, args);
        }
        catch (Throwable ex) {
            throw new IllegalArgumentException("Cannot instantiate : " + name, ex);
        }
        return instance;
    }

    public ClassLoader getClassLoader() {
        if (this.classLoader == null) {
            this.classLoader = ClassUtils.getDefaultClassLoader();
            return classLoader;
        }
        return this.classLoader;
    }

    /**
     * 将前端的数据转换为特定的类型
     * @param value
     * @param clazz
     * @return
     * @throws Exception
     */
    public Object convertType(String value, Class<?> clazz) throws Exception{
        if(clazz == Integer.class) {
            return Integer.valueOf(value);
        }
        /**  1995-02-12  */
        if(clazz == Date.class) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            // TODO
        }
        return value;
    }
}
