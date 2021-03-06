package tms.spring.service;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.util.WebUtils;
import tms.spring.dao.UserDao;
import tms.spring.entity.User;
import tms.spring.exception.MailException;
import tms.spring.exception.RegisterException;
import tms.spring.shiro.ShiroRealm;
import tms.spring.shiro.filter.ShiroFilterUtils;
import tms.spring.utils.MailCilent;
import tms.spring.utils.VerifyCodeUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Date;

/**
 * Created by Administrator on 2017/7/15 0015.
 */
@Service
public class LoginService {

    @Autowired
    private ShiroRealm shiroRealm;
    @Autowired
    private UserDao userDao;
    @Autowired
    private MailCilent mailCilent;

    public void login(HttpServletRequest request) throws Exception {
        String username=request.getParameter("username");
        String password=request.getParameter("password");
        String realValiteCode=request.getParameter("valiteCode");
        String rememberStr=request.getParameter("remember");
        if(realValiteCode==null||username==null||password==null||rememberStr==null){
            throw new Exception("填写的注册信息不完善");
        }
        Boolean remember=Boolean.getBoolean(rememberStr);
        String validateCode= (String) WebUtils.getSessionAttribute(request,VerifyCodeUtils.V_CODE);
        if(null==validateCode){
            throw new Exception("验证码失效");
        }
        if(!validateCode.equals(realValiteCode)){
            throw new Exception("验证码错误");
        }
        UsernamePasswordToken token = new UsernamePasswordToken(username, ShiroFilterUtils.encryptPassword(password));
        Subject subject = SecurityUtils.getSubject();
        token.setRememberMe(remember);
        subject.login(token);
    }

    public void logout(){
        shiroRealm.clearCachedAuthorizationInfo();
        SecurityUtils.getSubject().logout();
    }

    public void register(HttpServletRequest request) throws RegisterException {
        String realValiteCode=request.getParameter("valiteCode");
        String username=request.getParameter("username");
        String password=request.getParameter("password");
        String nickname=request.getParameter("nickname");
        String department=request.getParameter("department");
        String email=request.getParameter("email");
        if(realValiteCode==null||username==null||password==null||email==null){
            throw new RegisterException("填写的注册信息不完善");
        }
        String validateCode=String.valueOf(WebUtils.getSessionAttribute(request,email));
        if(null==validateCode){
            throw new RegisterException("验证码失效或与邮箱不匹配");
        }
        if(!validateCode.equals(realValiteCode)){
            throw new RegisterException("验证码错误");
        }
        synchronized (this){
            User userByName=userDao.selectUserByName(username);
            User userByEmail=userDao.selectUserByEmail(email);
            if(null!=userByName){
                throw new RegisterException("用户名已存在");
            }
            if(null!=userByEmail){
                throw new RegisterException("邮箱已存在");
            }
            User user=new User();
            user.setUsername(username);
            user.setPassword(ShiroFilterUtils.encryptPassword(password));
            user.setCreateTime(new Date());
            user.setDepartment(department);
            user.setEmail(email);
            user.setNickname(nickname);
            user.setStatus(User._1);
            userDao.insertUser(user);
        }
    }

    public void checkUserName(String username) throws Exception {
        User user=userDao.selectUserByName(username);
        if(null!=user){
            throw new Exception("查找用户名失败");
        }
    }

    public void checkEmail(String email) throws Exception {
        User user=userDao.selectUserByEmail(email);
        if(null!=user){
            throw new Exception("邮箱已被注册");
        }
    }

    public void sendEmailToGetValidateCode(HttpServletRequest request) throws MailException {
        String email=request.getParameter("email");
        if (email==null){
            throw new MailException("你输入的邮件地址为空");
        }
        String validateCode=mailCilent.sendHtmlMail(email);
        WebUtils.setSessionAttribute(request, email, validateCode);
    }

    public void updatePasswordByEmail(HttpServletRequest request) throws Exception {
        String realValiteCode=request.getParameter("valiteCode");
        String password=request.getParameter("password");
        String email=request.getParameter("email");
        if(realValiteCode==null||password==null||email==null){
            throw new Exception("填写的信息不完善");
        }
        String validateCode=String.valueOf(WebUtils.getSessionAttribute(request,email));
        if(null==validateCode){
            throw new RegisterException("验证码失效或与邮箱不匹配");
        }
        if(!validateCode.equals(realValiteCode)){
            throw new Exception("验证码错误");
        }
        User user=new User();
        user.setEmail(email);
        user.setPassword(password);
        userDao.updatePasswordByEmail(user);
    }
}
