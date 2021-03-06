package edu.ucar.unidata.cloudcontrol.controller.user;

import edu.ucar.unidata.cloudcontrol.config.SecurityConfig;
import edu.ucar.unidata.cloudcontrol.config.WebAppContext;
import edu.ucar.unidata.cloudcontrol.domain.User;
import edu.ucar.unidata.cloudcontrol.domain.UserBuilder;
import edu.ucar.unidata.cloudcontrol.service.user.UserManager;

import java.util.Arrays;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.nullValue;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Tests for edu.ucar.unidata.cloudcontrol.controller.user.DeleteUserController
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(classes = {WebAppContext.class, SecurityConfig.class})
public class DeleteUserControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private UserManager userManagerMock;

    @Autowired
    private WebApplicationContext context;

    @Before
    public void setUp() {
        reset(userManagerMock);

        mockMvc = MockMvcBuilders
            .webAppContextSetup(context)
            .apply(springSecurity())
            .build();
    }

    @Test
    @WithMockUser(username = "testUserOne", roles = {"USER"})
    public void deleteUserAccessToDeleteUserFormWithUnauthorizedUserShouldBeDenied() throws Exception {
        mockMvc.perform(get("/dashboard/user/delete/testUserOne").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("denied"))
            .andExpect(forwardedUrl("/WEB-INF/views/denied.jsp"));
            //.andDo(print());
    }


    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void deleteUserAccessToDeleteUserFormWithAdminAsAuthorizedUserShouldbeAllowed() throws Exception {
        mockMvc.perform(get("/dashboard/user/delete/testUserOne").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("dashboard"))
            .andExpect(forwardedUrl("/WEB-INF/views/dashboard.jsp"));
          //.andDo(print());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void deleteUserModelShouldContainUserObjectAndDeleteUserAction() throws Exception {
        Date now = new Date(System.currentTimeMillis());
        User testUserOne = new UserBuilder()
            .userId(1)
            .userName("testUserOne")
            .fullName("Test User One")
            .emailAddress("testUserOne@foo.bar")
            .password("password")
            .accessLevel(1)
            .accountStatus(1)
            .dateCreated(now)
            .dateModified(now)
            .build();

        when(userManagerMock.lookupUser("testUserOne")).thenReturn(testUserOne);
        mockMvc.perform(get("/dashboard/user/delete/testUserOne").with(csrf()))
            .andExpect(model().attribute("action", equalTo("deleteUser")))
            .andExpect(model().attribute("user", hasProperty("userId", is(1))))
            .andExpect(model().attribute("user", hasProperty("userName", is("testUserOne"))))
            .andExpect(model().attribute("user", hasProperty("fullName", is("Test User One"))))
            .andExpect(model().attribute("user", hasProperty("emailAddress", is("testUserOne@foo.bar"))))
            .andExpect(model().attribute("user", hasProperty("password", is("password"))))
            .andExpect(model().attribute("user", hasProperty("confirmPassword", nullValue())))
            .andExpect(model().attribute("user", hasProperty("accessLevel", is(1))))
            .andExpect(model().attribute("user", hasProperty("accountStatus", is(1))))
            .andExpect(model().attribute("user", hasProperty("dateCreated", is(now))))
            .andExpect(model().attribute("user", hasProperty("dateModified", is(now))));
          //.andDo(print());

        verify(userManagerMock, times(1)).lookupUser("testUserOne");
        verifyNoMoreInteractions(userManagerMock);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void deleteUserUserToDeletedNotFoundShouldDataRetrievalFailureException() throws Exception {
        when(userManagerMock.lookupUser("testUserOne")).thenThrow(new DataRetrievalFailureException("Unable to find User with userId 1"));
        mockMvc.perform(get("/dashboard/user/delete/testUserOne").with(csrf()))
            .andExpect(model().attribute("exception", isA(DataRetrievalFailureException.class)))
            .andExpect(status().isOk())
            .andExpect(view().name("fatalError"))
            .andExpect(forwardedUrl("/WEB-INF/views/fatalError.jsp"));
          //.andDo(print());

            verify(userManagerMock, times(1)).lookupUser("testUserOne");
            verifyNoMoreInteractions(userManagerMock);
    }

    @Test
    @WithMockUser(username = "testUserOne", roles = {"USER"})
    public void deleteUserPostsToDeleteUserFormWithUnauthorizedUserShouldBeDenied() throws Exception {
        mockMvc.perform(post("/dashboard/user/delete").with(csrf()))
            .andExpect(status().isOk())
            .andExpect(view().name("denied"))
            .andExpect(forwardedUrl("/WEB-INF/views/denied.jsp"));
       //   .andDo(print());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void deleteUserPostsToDeleteUserFormWithAdminAsAuthorizedUserShouldbeAllowed() throws Exception {
        mockMvc.perform(post("/dashboard/user/delete").with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(model().attribute("action", equalTo("listUsers")))
            .andExpect(redirectedUrl("/dashboard/user?action=listUsers"));
          //.andDo(print());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void deleteUserUserDeletedSuccessfullyModelShouldContainListOfUsersAndListUsersAction() throws Exception {
        User testUserOne = new UserBuilder()
            .userId(1)
            .userName("testUserOne")
            .build();
        User testUserTwo = new UserBuilder()
            .userId(2)
            .userName("testUserTwo")
            .build();

        when(userManagerMock.getUserList()).thenReturn(Arrays.asList(testUserOne, testUserTwo));
        doNothing().when(userManagerMock).deleteUser(testUserOne.getUserId());
        mockMvc.perform(post("/dashboard/user/delete").with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
            )
            .andExpect(status().is3xxRedirection())
            .andExpect(model().attribute("action", equalTo("listUsers")))
            .andExpect(redirectedUrl("/dashboard/user?action=listUsers"));
          //.andDo(print());

        verify(userManagerMock, times(1)).deleteUser(testUserOne.getUserId());
        verify(userManagerMock, times(1)).getUserList();
        verifyNoMoreInteractions(userManagerMock);
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    public void deleteUserUnableToDeletedUserShouldDataRetrievalFailureException() throws Exception {
        User testUserOne = new UserBuilder()
            .userId(1)
            .userName("testUserOne")
            .build();

        doThrow(new DataRetrievalFailureException("Unable to delete User. No User found with userId 1")).when(userManagerMock).deleteUser(testUserOne.getUserId());
        mockMvc.perform(post("/dashboard/user/delete").with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("userId", "1")
            )
            .andExpect(model().attribute("exception", isA(DataRetrievalFailureException.class)))
            .andExpect(status().isOk())
            .andExpect(view().name("fatalError"))
            .andExpect(forwardedUrl("/WEB-INF/views/fatalError.jsp"));
          //.andDo(print());
        verify(userManagerMock, times(1)).deleteUser(testUserOne.getUserId());
        verifyNoMoreInteractions(userManagerMock);
    }
}
