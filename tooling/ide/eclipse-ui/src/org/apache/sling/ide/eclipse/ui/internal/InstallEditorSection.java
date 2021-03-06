/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.eclipse.ui.internal;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.apache.sling.ide.eclipse.core.ISlingLaunchpadConfiguration;
import org.apache.sling.ide.eclipse.core.ISlingLaunchpadServer;
import org.apache.sling.ide.eclipse.core.SetBundleInstallLocallyCommand;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.wst.server.ui.editor.ServerEditorSection;

public class InstallEditorSection extends ServerEditorSection {
    protected boolean _updating;
    protected PropertyChangeListener _listener;

    private Button mvnSlingInstallButton;
    private Button quickLocalInstallButton;
    private ISlingLaunchpadServer launchpadServer;
    private PropertyChangeListener serverListener;

    @Override
    public void createSection(Composite parent) {
        super.createSection(parent);
        FormToolkit toolkit = getFormToolkit(parent.getDisplay());

        Section section = toolkit.createSection(parent, ExpandableComposite.TWISTIE | ExpandableComposite.EXPANDED
                | ExpandableComposite.TITLE_BAR | Section.DESCRIPTION | ExpandableComposite.FOCUS_TITLE);
        section.setText("Install");
        section.setDescription("Specify how to install artifacts to the launchpad instance");
        section.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_FILL));

        // ports
        Composite composite = toolkit.createComposite(section);

        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 8;
        layout.marginWidth = 8;
        composite.setLayout(layout);
        GridData gridData = new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.FILL_HORIZONTAL);
        composite.setLayoutData(gridData);
        toolkit.paintBordersFor(composite);
        section.setClient(composite);

        
        mvnSlingInstallButton = new Button(composite, SWT.RADIO);
        mvnSlingInstallButton.setText("Install bundles via mvn sling:install");
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        mvnSlingInstallButton.setLayoutData(data);
        
        quickLocalInstallButton = new Button(composite, SWT.RADIO);
        quickLocalInstallButton.setText("Install bundles directly from local directory");
        data = new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1);
        quickLocalInstallButton.setLayoutData(data);

        initialize();
    }

    public void init(IEditorSite site, IEditorInput input) {
        super.init(site, input);

        serverListener = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {

                if (ISlingLaunchpadServer.PROP_INSTALL_LOCALLY.equals(evt.getPropertyName())) {
            		quickLocalInstallButton.setSelection((Boolean)evt.getNewValue());
            		mvnSlingInstallButton.setSelection(!(Boolean)evt.getNewValue());
                }
            }
        };

        server.addPropertyChangeListener(serverListener);

        launchpadServer = (ISlingLaunchpadServer) server.getAdapter(ISlingLaunchpadServer.class);
        if (launchpadServer == null) {
            // TODO progress monitor
            launchpadServer = (ISlingLaunchpadServer) server.loadAdapter(ISlingLaunchpadServer.class,
                    new NullProgressMonitor());
        }
    }

    private void initialize() {

        final ISlingLaunchpadConfiguration config = launchpadServer.getConfiguration();

        quickLocalInstallButton.setSelection(config.bundleInstallLocally());
        mvnSlingInstallButton.setSelection(!config.bundleInstallLocally());

        SelectionListener listener = new SelectionAdapter() {
        	
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		execute(new SetBundleInstallLocallyCommand(server, quickLocalInstallButton.getSelection()));
        	}
		};

        quickLocalInstallButton.addSelectionListener(listener);
        mvnSlingInstallButton.addSelectionListener(listener);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.wst.server.ui.editor.ServerEditorSection#dispose()
     */
    @Override
    public void dispose() {
        if (server != null)
            server.removePropertyChangeListener(serverListener);

        super.dispose();
    }

}
