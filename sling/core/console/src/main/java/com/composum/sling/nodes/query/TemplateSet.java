package com.composum.sling.nodes.query;

import com.composum.sling.core.BeanContext;
import com.composum.sling.core.filter.ResourceFilter;
import com.composum.sling.core.filter.StringFilter;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TemplateSet extends ConfigSet<Template> {

    private static final Logger LOG = LoggerFactory.getLogger(TemplateSet.class);

    public static final String TEMPLATE_RESOURCE_TYPE = "composum/nodes/console/query/template";
    public static final String TEMPLATE_SET_RESOURCE_TYPE = "composum/nodes/console/query/template/set";

    public static final ResourceFilter EMPLATE_SET_ITEM_FILTER =
            new ResourceFilter.ResourceTypeFilter(new StringFilter.WhiteList("^composum/nodes/console/query/template$"));

    public TemplateSet(BeanContext context, Resource resource) {
        super(context, resource);
    }

    public TemplateSet(BeanContext context) {
        super(context);
    }

    public TemplateSet() {
        super();
    }

    protected String getSetResourceType() {
        return TEMPLATE_SET_RESOURCE_TYPE;
    }

    protected ResourceFilter getItemFilter() {
        return EMPLATE_SET_ITEM_FILTER;
    }

    protected Template createItem(Resource resource) {
        return new Template(context, resource);
    }
}
