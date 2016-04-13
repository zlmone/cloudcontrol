package edu.ucar.unidata.cloudcontrol.controller;

import javax.annotation.Resource;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.ui.Model;

import edu.ucar.unidata.cloudcontrol.domain.User;
import edu.ucar.unidata.cloudcontrol.service.UserManager;

/**
 * Controller to handle User authentication.
 */

@Controller
public class AuthenticationController {
    protected static Logger logger = Logger.getLogger(AuthenticationController.class);

    @Resource(name = "userManager")
    private UserManager userManager;

    /**
     * Accepts a GET request for the login page.
     * View is the login page.
     *
     * @param error The authentication error.
     * @param model The Model used by the View.
     * @return The 'login' path for the ViewResolver.
     */
    @RequestMapping(value = "/login/{error}", method = RequestMethod.GET)
    public String getLoginPage(@PathVariable String error, Model model) {
        if (error != null) {
			model.addAttribute("error", error);
        }
        return "login";
    }
	
    /**
     * Accepts a GET request for the login page.
     * View is the login page.
     *
     * @param model The Model used by the View.
     * @return The 'login' path for the ViewResolver.
     */
    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String getLoginPage(Model model) {
        return "login";
    }

    /**
     * Accepts a GET request for the logout page.
     * View is the logout page.
     *
     * @param model The Model used by the View.
     * @return The 'logout' path for the ViewResolver.
     */
    @RequestMapping(value = "/logout", method = RequestMethod.GET)
    public String getLogoutPage(Model model) {
        return "logout";
    }

    /**
     * Accepts a GET request for the denied page. This is shown whenever
     * a regular User tries to access an admin/User-specific only page.
     * View is a denied page.
     *
     * @return The 'denied' path for the ViewResolver.
     */
    @RequestMapping(value = "/denied", method = RequestMethod.GET)
    public String getDeniedPage() {
        return "denied";
    }

}
