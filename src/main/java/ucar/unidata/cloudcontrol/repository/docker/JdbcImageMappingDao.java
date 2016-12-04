package edu.ucar.unidata.cloudcontrol.repository.docker;

import java.util.List;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.support.JdbcDaoSupport;

import edu.ucar.unidata.cloudcontrol.domain.docker.ImageMapping;

/**
 * The ImageMappingDao implementation.  Persistence mechanism is a database. 
 */

public class JdbcImageMappingDao extends JdbcDaoSupport implements ImageMappingDao {
  
    protected static Logger logger = Logger.getLogger(JdbcImageMappingDao.class);

    private SimpleJdbcInsert insertActor;

    /**
     * Looks up and retrieves the ImageMapping from the persistence mechanism using the Image ID.
     * 
     * @param imageId   The ID of the Image (will be unique for each ImageMapping). 
     * @return  The ImageMapping.   
     */
    public ImageMapping lookupImageMapping(String imageId) {
        String sql = "SELECT * FROM imageMapping WHERE imageId = ?";
        List<ImageMapping> imageMappings = getJdbcTemplate().query(sql, new ImageMappingMapper(), imageId);        
        if (imageMappings.isEmpty()) {
            return null;
        }   
        return imageMappings.get(0);
    }
    
    /**
     * Looks up and retrieves a List of all ImageMappings from the persistence mechanism.
     * 
     * @return  The List of ImageMappings.   
     */
    public List<ImageMapping> getAllImageMappings() {
        String sql = "SELECT * FROM imageMapping";  
        List<ImageMapping> imageMappings = getJdbcTemplate().query(sql, new ImageMappingMapper());         
        return imageMappings;
    }

    /**
     * Finds and removes the ImageMapping from the persistence mechanism using the Image ID.
     * 
     * @param imageId  The ID of the Image. 
     */
    public void deleteImageMapping(String imageId) {
        String sql = "DELETE FROM imageMapping WHERE imageId = ?";
        int rowsAffected  = getJdbcTemplate().update(sql, imageId);
        if (rowsAffected <= 0) {
            throw new RecoverableDataAccessException("Unable to delete ImageMapping. No ImageMapping found with imageId: " + new Integer(imageId).toString());
        }   
    }
    
    /**
     * Creates a new ImageMapping in the persistence mechanism.
     * 
     * @param imageMapping  The ImageMapping to be created. 
     */
    public void createImageMapping(ImageMapping imageMapping) {
        this.insertActor = new SimpleJdbcInsert(getDataSource()).withTableName("imageMapping").usingGeneratedKeyColumns("id");
        SqlParameterSource parameters = new BeanPropertySqlParameterSource(imageMapping);
        Number newId = insertActor.executeAndReturnKey(parameters);
        if (newId != null) {
            imageMapping.setId(newId.intValue());  
        } else {
            throw new RecoverableDataAccessException("Unable to create ImageMapping: " + imageMapping.toString());
        }
    }


    /***
     * Maps each row of the ResultSet to a ImageMapping object.
     */
    private static class ImageMappingMapper implements RowMapper<ImageMapping> {
        /**
         * Maps each row of data in the ResultSet to the ImageMapping object.
         * 
         * @param rs  The ResultSet to be mapped.
         * @param rowNum  The number of the current row.
         * @return  The populated ImageMapping object.
         * @throws SQLException  If a SQLException is encountered getting column values.
         */
        public ImageMapping mapRow(ResultSet rs, int rowNum) throws SQLException {
            ImageMapping imageMapping = new ImageMapping();
            imageMapping.setId(rs.getInt("id"));
            imageMapping.setImageId(rs.getString("imageId"));
            return imageMapping;
        }
    }

}