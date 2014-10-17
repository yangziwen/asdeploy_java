package com.ablesky.asdeploy.action;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.struts2.convention.annotation.Action;
import org.apache.struts2.convention.annotation.Namespace;
import org.apache.struts2.convention.annotation.ParentPackage;
import org.apache.struts2.convention.annotation.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;

import com.ablesky.asdeploy.pojo.Role;
import com.ablesky.asdeploy.pojo.User;
import com.ablesky.asdeploy.pojo.UserRelRole;
import com.ablesky.asdeploy.service.IAuthorityService;
import com.ablesky.asdeploy.service.IUserService;
import com.ablesky.asdeploy.util.AuthUtil;

@ParentPackage("base")
@Namespace("/admin/user")
@SuppressWarnings("serial")
public class UserAdminAction extends ModelMapActionSupport {
	
	@Autowired
	private IUserService userService;
	@Autowired
	private IAuthorityService authorityService;
	
	@Action(value="list", results = {
		@Result(name="list", location="list.jsp")
	})
	public String list() {
		List<UserRelRole> superAdminRelList = authorityService.getUserRelRoleListResultByParam(0, 0, new ModelMap()
			.addAttribute("role_name", Role.NAME_SUPER_ADMIN)
		);
		ModelMap superAdminMap = new ModelMap();
		for(UserRelRole rel: superAdminRelList) {
			User user = rel.getUser();
			superAdminMap.put(user.getUsername(), user);
		}
		modelMap.put("list", userService.getUserListResult(0, 0, Collections.<String, Object>emptyMap()));
		modelMap.put("superAdminMap", superAdminMap);
		return "list";
	}
	
	@Action(value="switchSuperAdmin", results = {
		@Result(name="json", type="json", params={"root", "model"})
	})
	public String switchSuperAdmin() {
		Long userId = getFromModel("userId");
		Boolean isSuperAdmin = getFromModel("isSuperAdmin");
		if(userId == null || isSuperAdmin == null) {
			modelMap.put("success", false);
			modelMap.put("message", "参数有误!");
			return "json";
		}
		if(!AuthUtil.isSuperAdmin()) {
			modelMap.put("success", false);
			modelMap.put("message", "没有权限!");
			return "json";
		}
		if(userId.equals(AuthUtil.getCurrentUser().getId())) {
			modelMap.put("success", false);
			modelMap.put("message", "不允许超级管理员将自身将为普通用户!");
			return "json";
		}
		if(!isSuperAdmin) {
			authorityService.deleteUserRelRoleByUserIdAndRoleName(userId, Role.NAME_SUPER_ADMIN);
			modelMap.put("success", true);
			return "json";
		}
		User user = userService.getUserById(userId);
		Role role = authorityService.getRoleByName(Role.NAME_SUPER_ADMIN);
		UserRelRole superAdminRel = authorityService.addUserRelRoleByUserAndRole(user, role);
		if(superAdminRel == null){
			modelMap.put("success", false);
			modelMap.put("message", "用户或角色不存在!");
			return "json";
		}
		modelMap.put("success", true);
		return "json";
	}
	
	@Action(value="changePassword", results = {
		@Result(name="changePassword", location="changePassword.jsp"),
		@Result(name="json", type="json", params={"root", "model"})
	})
	public String changePassword() {
		Long userId = NumberUtils.toLong(this.<String>getFromModel("userId"));
		String method = request.getMethod();
		if(HttpMethod.POST.equals(method)) {
			doChangePassword(userId, this.<String>getFromModel("newPassword"));
			return "json";
		} else {
			modelMap.put("user", userService.getUserById(userId));
			return "changePassword";
		}
	}
	
	private void doChangePassword(Long userId, String newPassword) {
		if(!AuthUtil.isSuperAdmin()) {
			modelMap.put("success", false);
			modelMap.put("message", "没有权限!");
			return;
		}
		if(userId == null || userId <= 0 || StringUtils.isBlank(newPassword)) {
			modelMap.put("success", false);
			modelMap.put("message", "参数有误!");
			return;
		}
		User user = userService.getUserById(userId);
		if(user == null) {
			modelMap.put("success", false);
			modelMap.put("message", "用户不存在!");
			return;
		}
		user.setPassword(AuthUtil.hashPassword(user.getUsername(), newPassword));
		userService.saveOrUpdateUser(user);
		modelMap.put("success", true);
		modelMap.put("message", "修改成功!"); 
		return;
	}
}