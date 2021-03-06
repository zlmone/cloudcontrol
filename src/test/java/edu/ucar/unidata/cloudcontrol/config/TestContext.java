package edu.ucar.unidata.cloudcontrol.config;

import org.mockito.Mockito;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import edu.ucar.unidata.cloudcontrol.service.docker.ClientManager;
import edu.ucar.unidata.cloudcontrol.service.docker.ContainerManager;
import edu.ucar.unidata.cloudcontrol.service.docker.ContainerMappingManager;
import edu.ucar.unidata.cloudcontrol.service.docker.ImageManager;
import edu.ucar.unidata.cloudcontrol.service.docker.ImageMappingManager;
import edu.ucar.unidata.cloudcontrol.service.docker.ServerManager;
import edu.ucar.unidata.cloudcontrol.service.docker.validators.ClientConfigValidator;
import edu.ucar.unidata.cloudcontrol.service.user.UserManager;
import edu.ucar.unidata.cloudcontrol.service.user.validators.CreateUserValidator;
import edu.ucar.unidata.cloudcontrol.service.user.validators.EditUserValidator;
import edu.ucar.unidata.cloudcontrol.service.user.validators.PasswordValidator;


/**
 * Mock object dependancies used for controller tests (found via component scan in applicationContext-test.xml)
 */
@Configuration
public class TestContext {

    /*
     * edu.ucar.unidata.cloudcontrol.service.docker dependancies
     */

    @Bean
    public ClientManager clientManager() {
        return Mockito.mock(ClientManager.class);
    }

    @Bean
    public ContainerManager containerManager() {
        return Mockito.mock(ContainerManager.class);
    }

    @Bean
    public ServerManager serverManager() {
        return Mockito.mock(ServerManager.class);
    }

    @Bean
    public ImageManager imageManager() {
        return Mockito.mock(ImageManager.class);
    }

    @Bean
    public ClientConfigValidator clientConfigValidator() {
        return Mockito.mock(ClientConfigValidator.class);
    }

    @Bean
    public ImageMappingManager imageMappingManager() {
        return Mockito.mock(ImageMappingManager.class);
    }

    @Bean
    public ContainerMappingManager containerMappingManager() {
        return Mockito.mock(ContainerMappingManager.class);
    }

    /*
     * edu.ucar.unidata.cloudcontrol.service.user dependancies
     */

    @Bean
    public UserManager userManager() {
        return Mockito.mock(UserManager.class);
    }

    @Bean
    public CreateUserValidator createUserValidator() {
        return Mockito.mock(CreateUserValidator.class);
    }

    @Bean
    public EditUserValidator editUserValidator() {
        return Mockito.mock(EditUserValidator.class);
    }

    @Bean
    public PasswordValidator passwordValidator() {
        return Mockito.mock(PasswordValidator.class);
    }
}
