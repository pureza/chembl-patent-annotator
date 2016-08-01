package uk.ac.ebi.chembl.storage.dao;


import org.skife.jdbi.v2.sqlobject.SqlQuery;
import org.skife.jdbi.v2.sqlobject.customizers.RegisterMapper;
import org.skife.jdbi.v2.sqlobject.stringtemplate.UseStringTemplate3StatementLocator;
import org.skife.jdbi.v2.unstable.BindIn;
import uk.ac.ebi.chembl.model.PatentXml;

import java.util.Iterator;
import java.util.List;


/**
 * Data Access Object for Patent XML in the Alexandria database
 */
@RegisterMapper(PatentXmlMapper.class)
@UseStringTemplate3StatementLocator
public interface PatentXmlDao {


    @SqlQuery("SELECT ucid AS patent_number, xml.f_patent_document(xml.f_ucid2id(ucid), null) AS content " +
            "    FROM xml.t_patent_document_values " +
            "   WHERE ucid IN (<patentNumbers>)")
    Iterator<PatentXml> retrieveBatch(@BindIn("patentNumbers") List<String> patentNumbers);

    void close();
}
