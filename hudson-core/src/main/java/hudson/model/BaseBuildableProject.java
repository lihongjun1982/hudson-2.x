/*
 * The MIT License
 *
 * Copyright (c) 2011, Oracle Corporation, Anton Kozak, Nikita Levyankov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.model;

import hudson.Functions;
import hudson.model.Descriptor.FormException;
import hudson.tasks.BuildStep;
import hudson.tasks.BuildWrapper;
import hudson.tasks.Builder;
import hudson.tasks.Publisher;
import hudson.triggers.Trigger;
import hudson.util.DescribableList;
import hudson.util.DescribableListUtil;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import net.sf.json.JSONObject;
import org.hudsonci.api.model.IProject;
import org.hudsonci.model.project.property.BaseProjectProperty;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Base buildable project.
 *
 * @author Anton Kozak.
 */
public abstract class BaseBuildableProject<P extends BaseBuildableProject<P,B>,B extends Build<P,B>>
    extends AbstractProject<P, B>
    implements Saveable, BuildableItemWithBuildWrappers, IProject {

    public static final String BUILDERS_PROPERTY_NAME = "builders";
    public static final String BUILD_WRAPPERS_PROPERTY_NAME = "buildWrappers";


    /**
     * List of active {@link Builder}s configured for this project.
     *
     * @deprecated as of 2.2.0
     *             don't use this field directly, logic was moved to {@link org.hudsonci.api.model.IProjectProperty}.
     *             Use getter/setter for accessing to this field.
     */
    @Deprecated
    private DescribableList<Builder,Descriptor<Builder>> builders =
            new DescribableList<Builder,Descriptor<Builder>>(this);

    /**
     * List of active {@link Publisher}s configured for this project.
     *
     * @deprecated as of 2.2.0
     *             don't use this field directly, logic was moved to {@link org.hudsonci.api.model.IProjectProperty}.
     *             Use getter/setter for accessing to this field.
     */
    @Deprecated
    private DescribableList<Publisher,Descriptor<Publisher>> publishers =
            new DescribableList<Publisher,Descriptor<Publisher>>(this);

    /**
     * List of active {@link BuildWrapper}s configured for this project.
     *
     * @deprecated as of 2.2.0
     *             don't use this field directly, logic was moved to {@link org.hudsonci.api.model.IProjectProperty}.
     *             Use getter/setter for accessing to this field.
     */
    @Deprecated
    private DescribableList<BuildWrapper,Descriptor<BuildWrapper>> buildWrappers =
            new DescribableList<BuildWrapper,Descriptor<BuildWrapper>>(this);

    /**
     * Creates a new project.
     * @param parent parent {@link ItemGroup}.
     * @param name the name of the project.
     */
    public BaseBuildableProject(ItemGroup parent, String name) {
        super(parent, name);
    }

    @Override
    public void onLoad(ItemGroup<? extends Item> parent, String name) throws IOException {
        super.onLoad(parent, name);

        getBuildersList().setOwner(this);
        getPublishersList().setOwner(this);
        getBuildWrappersList().setOwner(this);
    }

    @Override
    protected void buildProjectProperties() throws IOException {
        super.buildProjectProperties();
        convertBuildersProjectProperty();
        convertBuildWrappersProjectProperties();
        convertPublishersProperties();
    }

    protected void buildDependencyGraph(DependencyGraph graph) {
        getPublishersList().buildDependencyGraph(this,graph);
        getBuildersList().buildDependencyGraph(this,graph);
        getBuildWrappersList().buildDependencyGraph(this,graph);
    }

    @Override
    protected List<Action> createTransientActions() {
        List<Action> r = super.createTransientActions();

        for (BuildStep step : getBuildersList())
            r.addAll(step.getProjectActions(this));
        for (BuildStep step : getPublishersList())
            r.addAll(step.getProjectActions(this));
        for (BuildWrapper step : getBuildWrappersList())
            r.addAll(step.getProjectActions(this));
        for (Trigger trigger : getTriggersList())
            r.addAll(trigger.getProjectActions());

        return r;
    }

    /**
     * @inheritDoc
     */
    public List<Builder> getBuilders() {
        return getBuildersList().toList();
    }

    /**
     * @inheritDoc
     */
    @SuppressWarnings("unchecked")
    public DescribableList<Builder,Descriptor<Builder>> getBuildersList() {
        return getDescribableListProjectProperty(BUILDERS_PROPERTY_NAME).getValue();
    }

    /**
     * @inheritDoc
     */
    public void setBuilders(DescribableList<Builder,Descriptor<Builder>> builders) {
        getDescribableListProjectProperty(BUILDERS_PROPERTY_NAME).setValue(builders);
    }

    /**
     * @inheritDoc
     */
    public Map<Descriptor<Publisher>,Publisher> getPublishers() {
        return getPublishersList().toMap();
    }

    public Publisher getPublisher(Descriptor<Publisher> descriptor) {
        return (Publisher) getBaseProjectProperty(descriptor.getJsonSafeClassName()).getValue();
    }
    /**
     * Returns the list of the publishers available in the hudson.
     *
     * @return the list of the publishers available in the hudson.
     */
    @SuppressWarnings("unchecked")
    public DescribableList<Publisher, Descriptor<Publisher>> getPublishersList() {
        List<Descriptor<Publisher>> descriptors = Functions.getPublisherDescriptors(this);
        List<Publisher> publisherList = new CopyOnWriteArrayList<Publisher>();
        DescribableList<Publisher, Descriptor<Publisher>> result
            = new DescribableList<Publisher, Descriptor<Publisher>>(this);
        for (Descriptor<Publisher> descriptor : descriptors) {
            BaseProjectProperty<Publisher> property = getBaseProjectProperty(descriptor.getJsonSafeClassName());
            if (null != property.getValue()) {
                publisherList.add(property.getValue());
            }
        }
        result.addAllTo(publisherList);
        return result;
    }

    /**
     * @inheritDoc
     */
    public Map<Descriptor<BuildWrapper>,BuildWrapper> getBuildWrappers() {
        return getBuildWrappersList().toMap();
    }

    /**
     * @inheritDoc
     */
    @SuppressWarnings("unchecked")
    public DescribableList<BuildWrapper, Descriptor<BuildWrapper>> getBuildWrappersList() {
        return getDescribableListProjectProperty(BUILD_WRAPPERS_PROPERTY_NAME).getValue();
    }

    /**
     * Sets build wrappers.
     *
     * @param buildWrappers buildWrappers.
     */
    public void setBuildWrappers(DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers) {
        getDescribableListProjectProperty(BUILD_WRAPPERS_PROPERTY_NAME).setValue(buildWrappers);
    }

    /**
     * Builds publishers.
     * @param req {@link StaplerRequest}
     * @param json {@link JSONObject}
     * @param descriptors list of descriptors.
     * @throws hudson.model.Descriptor.FormException
     */
    @SuppressWarnings("unchecked")
    protected void buildPublishers( StaplerRequest req, JSONObject json, List<Descriptor<Publisher>> descriptors) throws FormException{
        for (Descriptor<Publisher> d : descriptors) {
            String name = d.getJsonSafeClassName();
            BaseProjectProperty<Publisher> baseProperty = getBaseProjectProperty(name);
            Publisher publisher = null;
            if (json.has(name)) {
                publisher = d.newInstance(req, json.getJSONObject(name));
            }
            baseProperty.setValue(publisher);
        }
    }

    protected void convertPublishersProperties() {
        if (null != publishers) {
            putAllProjectProperties(DescribableListUtil.convertToProjectProperties(publishers, this), false);
            publishers = null;
        }
    }

    protected void convertBuildWrappersProjectProperties() {
        if (null == getProperty(BUILD_WRAPPERS_PROPERTY_NAME)) {
            setBuildWrappers(buildWrappers);
            buildWrappers = null;
        }
    }

    protected void convertBuildersProjectProperty() {
        if (null == getProperty(BUILDERS_PROPERTY_NAME)) {
            setBuilders(builders);
            builders = null;
        }
    }
}
