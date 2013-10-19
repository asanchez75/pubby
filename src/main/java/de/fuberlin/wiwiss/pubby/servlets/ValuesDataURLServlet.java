package de.fuberlin.wiwiss.pubby.servlets;
import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDFS;

import de.fuberlin.wiwiss.pubby.Configuration;
import de.fuberlin.wiwiss.pubby.HypermediaResource;
import de.fuberlin.wiwiss.pubby.MappedResource;
import de.fuberlin.wiwiss.pubby.ModelResponse;
import de.fuberlin.wiwiss.pubby.ResourceDescription;
import de.fuberlin.wiwiss.pubby.vocab.FOAF;

/**
 * A servlet for serving an RDF document describing resources
 * related to a given resource via a given property. The property
 * can be a forward or backward arc.
 * 
 * @author Richard Cyganiak (richard@cyganiak.de)
 * @version $Id$
 */
public class ValuesDataURLServlet extends ValuesBaseServlet {

	public boolean doGet(HypermediaResource controller,
			Collection<MappedResource> resources, Property property, boolean isInverse, 
			HttpServletRequest request,
			HttpServletResponse response,
			Configuration config) throws IOException {

		Model descriptions = listPropertyValues(resources, property, isInverse);
		if (descriptions.isEmpty()) return false;
		
		// Add document metadata
		if (descriptions.qnameFor(FOAF.primaryTopic.getURI()) == null
				&& descriptions.getNsPrefixURI("foaf") == null) {
			descriptions.setNsPrefix("foaf", FOAF.NS);
		}
		if (descriptions.qnameFor(RDFS.label.getURI()) == null
				&& descriptions.getNsPrefixURI("rdfs") == null) {
			descriptions.setNsPrefix("rdfs", RDFS.getURI());
		}
		Resource r = descriptions.getResource(controller.getAbsoluteIRI());
		Resource document = descriptions.getResource(
				addQueryString(
						isInverse 
								? controller.getInverseValuesDataURL(property) 
								: controller.getValuesDataURL(property), request));
		document.addProperty(FOAF.primaryTopic, r);
		String resourceLabel = new ResourceDescription(controller, descriptions, config).getTitle();
		String propertyLabel = config.getPrefixes().qnameFor(property.getURI());
		document.addProperty(RDFS.label, 
				getDocumentTitle(resourceLabel, propertyLabel, isInverse));
		for (MappedResource resource: resources) {
			resource.getDataset().addDocumentMetadata(descriptions, document);
		}
		
		new ModelResponse(descriptions, request, response).serve();
		return true;
	}

	public String getDocumentTitle(String resourceLabel, String propertyLabel,
			boolean isInverse) {
		if (isInverse) {
			return "RDF description of all resources whose " + propertyLabel + " is " + resourceLabel;
		} else { 
			return "RDF description of all values that are " + propertyLabel + " of " + resourceLabel;
		}
	}

	private static final long serialVersionUID = -7927775670218866340L;
}