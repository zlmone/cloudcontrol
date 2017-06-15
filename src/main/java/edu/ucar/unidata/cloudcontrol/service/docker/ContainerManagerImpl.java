package edu.ucar.unidata.cloudcontrol.service.docker;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;

import org.apache.log4j.Logger;

import org.springframework.dao.RecoverableDataAccessException;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;

import edu.ucar.unidata.cloudcontrol.domain.docker.ContainerMapping;
import edu.ucar.unidata.cloudcontrol.domain.docker._Container;
import edu.ucar.unidata.cloudcontrol.service.docker.ClientManager;
import edu.ucar.unidata.cloudcontrol.service.docker.ContainerMappingManager;
import edu.ucar.unidata.cloudcontrol.service.docker.converters.ContainerConverter;

/**
 * Service for processing Docker container-related api requests.
 */
public class ContainerManagerImpl implements ContainerManager {
    
    protected static Logger logger = Logger.getLogger(ContainerManagerImpl.class);

    @Resource(name = "clientManager")
    private ClientManager clientManager;
    
    @Resource(name = "containerMappingManager")
    private ContainerMappingManager containerMappingManager;
    
    /**
     * Requests a List of all available containers.
     *
     * @return  A List edu.ucar.unidata.cloudcontrol.domain.docker._Container objects.
     */
    public List<_Container> getContainerList() {
        List<_Container> _containers;
        try {
            DockerClient dockerClient = clientManager.initializeDockerClient();
            List<Container> containers = dockerClient.listContainersCmd().withShowAll(true).exec();
            ContainerConverter containerConverter = new ContainerConverter();
            _containers = containerConverter.processContainerList(containers);
        } catch (Exception e) {
            logger.error("Unable to get list of Containers: " + e);
            _containers = null;
        }           
        return _containers;
    }
    
    /**
     * Requests a List of all available containers corresponding to an image.
     *
     * @param imageId  The container's Image ID.
     * @return  A List of edu.ucar.unidata.cloudcontrol.domain.docker._Container objects.
     */
    public List<_Container> getContainerListByImage(String imageId) {
        List<_Container> _containerList = null;
        List<_Container> _containers = getContainerList();
        if (_containers != null) {    
            _Container _container = null;
            _containerList = new ArrayList<_Container>();
            for (_Container c : _containers) {
                if (imageId.equals(c.getImageId())) {
                    _container = c; 
                    _containerList.add(_container);
                }
            } 
        }
        return _containerList;
    }

    /**
     * Requests a List of all available RUNNING containers corresponding to an image.
     * possible status types: created|restarting|running|paused|exited
     *
     * @param imageId  The container's Image ID.
     * @return  A List of running edu.ucar.unidata.cloudcontrol.domain.docker._Container objects.
     */
    public List<_Container> getRunningContainerListByImage(String imageId) {
        List<_Container> _containerList = null;
        List<_Container> _containers = getContainerList();  
        if (_containers!= null) { 
            _Container _container = null; 
            _containerList = new ArrayList<_Container>();
            for (_Container c : _containers) {
                if (imageId.equals(c.getImageId())) {
                    if (!StringUtils.contains(c.getStatus(), "Exited")) {
                        if (!StringUtils.contains(c.getStatus(), "Created")) {
                            if (!StringUtils.contains(c.getStatus(), "Paused")) {
                                _container = c; 
                                _containerList.add(_container);
                            }
                        }
                    }
                }
            } 
        }
        return _containerList;
    }
    
    /**
     * Requests a Map of container statuses.
     *
     * @return  A Map of container statuses.
     */
    public Map<String, String> getContainerStatusMap() {
        Map<String, String> _containerStatusMap = null;
        List<_Container> _containers = getContainerList();  
        if (_containers != null) { 
            _containerStatusMap = new HashMap<String, String>();
            for (_Container c : _containers) {
                _containerStatusMap.put(c.getImage(), c.getStatus());
            }
        } 
        return _containerStatusMap;
    }

    /**
     * Requests a single _Container object.
     * 
     * @param containerId  The container ID.
     * @return  The edu.ucar.unidata.cloudcontrol.domain.docker._Container object.   
     */
    public _Container getContainer(String containerId) {
        List<_Container> _containers = getContainerList();  
        _Container _container = null;
        for (_Container c : _containers) {
            if (containerId.equals(c.getId())) {
                _container = c; 
                break;
            }
        } 
        return _container;
    }  
    
    /**
     * Starts a Docker container.
     *
     * @param imageId  The ID of the image in which to start the container.
     * @param authenticatedUser  The user that the started container.
     * @return  The ID of the container that has been started (or null if unsuccessful). 
     */
    public String startContainer(String imageId, String authenticatedUser) {
        String containerId = null;
        try {
            DockerClient dockerClient = clientManager.initializeDockerClient();
            CreateContainerResponse createContainerResponse = createContainer(dockerClient, imageId); 
            containerId = createContainerResponse.getId().substring(0,12);
            dockerClient.startContainerCmd(containerId).exec(); 
            
            // further checking to see if the container is running
            InspectContainerResponse inspectContainerResponse = inspectContainer(dockerClient, containerId);
            if (!inspectContainerResponse.getState().getStatus().equals("running")) {
                containerId = null;
                logger.error("Container " + containerId + " is not running when it should be: " + inspectContainerResponse.getState().getExitCode());
            } else {
                try {
					// add mapping 
                    ContainerMapping containerMapping = new ContainerMapping(); 
                    containerMapping.setContainerId(containerId);
                    containerMapping.setImageId(imageId);
                    containerMapping.setUserName(authenticatedUser);
                    containerMapping.setDatePerformed(new Date());
                    containerMappingManager.createContainerMapping(containerMapping);
				} catch (RecoverableDataAccessException e) {
					// can't add mapping, stop the container
					logger.info("Stopping container " + containerId + ": " + e);
					stopContainer(containerId);
				}
            }   
        } catch (NotModifiedException e) {
            logger.error("Container " + containerId + " is already running: " + e);    
        } catch (NotFoundException e) {
            logger.error("Unable to find and start container " + containerId + ": " + e);
        } catch (Exception e) {
            logger.error("Unable to start container " + containerId + ": " + e);
        } 
        return containerId;          
    }   
    
    /**
     * Stops a Docker container.
     *
     * @param containerId  The ID of the container to stop.
     * @return  The whether the container has been started or not. 
     */
    public boolean stopContainer(String containerId) {
        boolean isStopped = false;
        try {        
            DockerClient dockerClient = clientManager.initializeDockerClient();    
            dockerClient.stopContainerCmd(containerId).exec();
            
            // further checking to confirm the container has stopped running
            if (containerIsRunning(dockerClient, containerId)) {
                logger.error("Container " + containerId + " is still running when it should not be.");
            } else {
                // remove the container completely
                if (removeSingleContainerFromImage(getContainer(containerId).getImageId(), containerId)) {
                    isStopped = true;
                } else {
                    logger.error("Unable stop and remove container " + containerId);   
                }
            }
        } catch (NotFoundException e) {
            logger.error("Unable to find and stop container " + containerId + ": " + e);
        } catch (Exception e) {
            logger.error("Unable to stop container " + containerId + ": " + e);
        }
        return isStopped;        
    }

    /**
     * Requests whether the edu.ucar.unidata.cloudcontrol.domain.docker._Container is running or not.
     *
     * @param id  The ID of the _Container to check.
     * @param dockerClient  The initialized DockerClient to use.
     * @return  The whether the container is running or not. 
     */
    public boolean containerIsRunning(DockerClient dockerClient, String id){
        boolean isStopped = false;
        try {        
            InspectContainerResponse inspectContainerResponse = inspectContainer(dockerClient, id);
            if (inspectContainerResponse.getState().getStatus().equals("running")) {
                return true;
            } 
        } catch (NotModifiedException e) {
            logger.error("Container is still running: " + e);     
        } catch (Exception e) {
            logger.error("Unable to stop Container: " + e);
        }
        return isStopped;   
	}
	
	
     /**
      * Returns a CreateContainerResponse object.
      *
      * @param dockerClient  The DockerClient object to use.
      * @param imageId  The ID of the image we want container-ize.
      * @return  The CreateContainerResponse.
      */
      public CreateContainerResponse createContainer(DockerClient dockerClient, String imageId) {
          try {
              return dockerClient.createContainerCmd(imageId).exec();
          } catch (Exception e) {
              logger.error("Unable to create a Container: " + e);
              return null;
          }    
      }
     
     /**
      * Returns a requested InspectContainerResponse.
      *
      * @param dockerClient  The DockerClient object to use.
      * @param containerId  The Container ID to inspect.
      * @return  The InspectContainerResponse.
      */
     public InspectContainerResponse inspectContainer(DockerClient dockerClient, String containerId) {
         try {
             return dockerClient.inspectContainerCmd(containerId).exec();
         } catch (Exception e) {
             logger.error("Unable to inspect Container: " + e);
             return null;
         }    
     }  
     
     /**
      * Removes all Containers from an Image.
      *
      * @param imageId  The ID of the Image whose Containers need to be removed.
      * @return  The whether the Containers were successfully removed or not. 
      */
     public boolean removeContainersFromImage(String imageId) {
         boolean hasBeenRemoved = false;
         try {
             DockerClient dockerClient = clientManager.initializeDockerClient();
             List<_Container> _containers = getContainerListByImage(imageId);
             if (!_containers.isEmpty()) {
                 for (_Container _container : _containers) {
                     if (containerIsRunning(dockerClient, _container.getId())) {
                         if (!stopContainer(_container.getId())) {
                             logger.error("Unable to stop container:  " + _container.getId() + ". Will attempt to force the container removal.");
                         }
                     }
                     dockerClient.removeContainerCmd(_container.getId()).withForce(true).exec();
                     _Container _c = getContainer(_container.getId());
                     if (_c == null) {
                         hasBeenRemoved = true;
                     } 
                 }
             } else {
                 hasBeenRemoved = true;
             }   
         } catch (Exception e) {
             logger.error("Unable to remove Container: " + e);
         }
         return hasBeenRemoved;
     }
     
     /**
      * Removes a single Container from an Image.
      *
      * @param imageId  The ID of the Image corresponding to the Container.
      * @param containerId  The ID of the Container to remove.
      * @return  The whether the Container was successfully removed or not. 
      */
     public boolean removeSingleContainerFromImage(String imageId, String containerId) {
         boolean hasBeenRemoved = false;
         try {
             DockerClient dockerClient = clientManager.initializeDockerClient();
             List<_Container> _containers = getContainerListByImage(imageId);
             if (!_containers.isEmpty()) {
                 for (_Container _container : _containers) {
                     if (_container.getId().equals(containerId)) {
                         
                         if (containerIsRunning(dockerClient, _container.getId())) {
                             if (!stopContainer(_container.getId())) {
                                 logger.error("Unable to stop container:  " + _container.getId() + ". Will attempt to force the container removal.");
                             }
                         }
                         dockerClient.removeContainerCmd(_container.getId()).withForce(true).exec();
                         _Container _c = getContainer(_container.getId());
                         if (_c == null) {
                             hasBeenRemoved = true;
                         }
                     }
                 }
             } else {
                 logger.error("Unable to find and remove container");   
             }   
         } catch (NotFoundException e) {
             logger.error("Unable to find and remove container: " + e);   
         } catch (Exception e) {
             logger.error("Unable to remove container: " + e);
         }
         return hasBeenRemoved;
     }
}