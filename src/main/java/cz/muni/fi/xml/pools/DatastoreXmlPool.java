package cz.muni.fi.xml.pools;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cz.muni.fi.scheduler.elementpools.IDatastorePool;
import cz.muni.fi.scheduler.elements.DatastoreElement;
import cz.muni.fi.xml.mappers.DatastoreXmlMapper;
import cz.muni.fi.xml.resources.lists.DatastoreXmlList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.factory.Mappers;

/**
 * This class is used for loading the XML documents provided for the testing mode.
 * Datastores in the XML document are mapped to DatastoreElements.
 * These elements are stored in the list containing all the elements in the XML document.
 * 
 * @author Gabriela Podolnikova
 */
public class DatastoreXmlPool implements IDatastorePool {
    
    private List<DatastoreElement> datastores;
    
    DatastoreXmlMapper datastoreXmlMapper = Mappers.getMapper(DatastoreXmlMapper.class);

    public DatastoreXmlPool(String dsPoolPath) throws IOException {
        XmlMapper xmlMapper = new XmlMapper();
        String dsPoolMessage = new String(Files.readAllBytes(Paths.get(dsPoolPath)));
        DatastoreXmlList xmlList = xmlMapper.readValue(dsPoolMessage, DatastoreXmlList.class);
        datastores = datastoreXmlMapper.map(xmlList.getDatastores());
    }
                    
    @Override
    public List<DatastoreElement> getDatastores() {
        return Collections.unmodifiableList(datastores);
    }

    @Override
    public DatastoreElement getDatastore(int id) {
        for (DatastoreElement ds : datastores) {
            if (ds.getId() == id) {
                return ds;
            }
        }
        return null;
    }

    @Override
    public List<DatastoreElement> getSystemDs() {
        return getDatastores().stream().filter(ds -> ds.getType() == 1).collect(Collectors.toList());
    }

    @Override
    public List<Integer> getDatastoresIds() {
        return getDatastores().stream().map(DatastoreElement::getId).collect(Collectors.toList());
    }

    @Override
    public List<DatastoreElement> getImageDs() {
        return getDatastores().stream().filter(ds -> ds.getType() == 0).collect(Collectors.toList());
    }
    
}
