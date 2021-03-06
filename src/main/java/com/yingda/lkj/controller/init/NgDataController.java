package com.yingda.lkj.controller.init;

import com.yingda.lkj.beans.entity.system.Menu;
import com.yingda.lkj.beans.entity.system.Permission;
import com.yingda.lkj.beans.entity.system.User;
import com.yingda.lkj.beans.exception.CustomException;
import com.yingda.lkj.beans.pojo.system.NgAppData;
import com.yingda.lkj.beans.pojo.system.NgAppDetail;
import com.yingda.lkj.beans.pojo.system.NgMenu;
import com.yingda.lkj.beans.pojo.system.NgUser;
import com.yingda.lkj.controller.BaseController;
import com.yingda.lkj.service.system.*;
import com.yingda.lkj.utils.StreamUtil;
import com.yingda.lkj.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author hood  2020/11/11
 */
@Controller
@RequestMapping("/init/ngData")
public class NgDataController extends BaseController {

    @Autowired
    private MenuService menuService;
    @Autowired
    private AuthService authService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private PermissionAuthService permissionAuthService;

    @RequestMapping("")
    @ResponseBody
    public NgAppData appData() throws CustomException {
        User user = getUser();

        NgAppDetail ngAppDetail = new NgAppDetail();
        List<NgMenu> ngMenus = menuService.jsonifiedAsNgMenu(menuService.showDown());
        ngMenus = ngMenus.stream().sorted(Comparator.comparing(NgMenu::getSeq)).collect(Collectors.toList());

        if (StringUtils.isEmpty(user)) {
            NgUser ngUser = new NgUser();
            return new NgAppData(ngAppDetail, ngUser, ngMenus);
        }

        List<Menu> valuableMenus = authService.getValuableMenus(user);
        List<String> menuIds = StreamUtil.getList(valuableMenus, Menu::getId);

        List<Permission> valuablePermissions = permissionAuthService.getByUser(user);
        List<String> permissionIds = StreamUtil.getList(valuablePermissions, Permission::getId);
        menuIds.addAll(permissionIds);

        NgUser ngUser = new NgUser(user, roleService.getRole(user.getRoleId()));
        ngUser.setMenuIds(menuIds);

        return new NgAppData(ngAppDetail, ngUser, ngMenus);
    }

}
