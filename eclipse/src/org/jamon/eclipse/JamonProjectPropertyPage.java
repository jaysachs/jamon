package org.jamon.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.PropertyPage;

public class JamonProjectPropertyPage extends PropertyPage {

	public JamonProjectPropertyPage() {
		super();
	}

	private Button isJamonProjectCheckbox;
	
	private IJavaProject getJavaProject() throws CoreException {
		IProject project = (IProject) (this.getElement().getAdapter(IProject.class));
		return (IJavaProject) (project.getNature(JavaCore.NATURE_ID));
	}
	
	private void addFirstSection(Composite parent) {
			Composite isJamonProjectGroup = new Composite(parent,SWT.NONE);
			isJamonProjectGroup.setLayout(new GridLayout(3, false));
			isJamonProjectGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

			// project location entry field
			isJamonProjectCheckbox = new Button(isJamonProjectGroup, SWT.CHECK | SWT.LEFT);
			isJamonProjectCheckbox.setText("Is Jamon Project");
			isJamonProjectCheckbox.setEnabled(true);

			try {		
				isJamonProjectCheckbox.setSelection(JamonNature.projectHasNature(getJavaProject().getProject()));
			} catch (CoreException ex) {
				ex.printStackTrace();
			}
	}
	
	private void addSeparator(Composite parent) {
		Label separator = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		GridData gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		separator.setLayoutData(gridData);
	}
	
	private Text templateSourceText;
	private static final String DEFAULT_TEMPLATE_SOURCE = "templates";
	private static final String TEMPLATE_SOURCE_PROPERTY = "TEMPLATE_SOURCE_FOLDER";

	private void addSecondSection(Composite parent) {
		Composite composite = createDefaultComposite(parent);

		// Label for owner field
		Label templateSourceLabel = new Label(composite, SWT.NONE);
		templateSourceLabel.setText("Template source folder:");
		// Owner text field
		templateSourceText = new Text(composite, SWT.SINGLE | SWT.BORDER);
		GridData gd = new GridData();
		gd.widthHint = convertWidthInCharsToPixels(50);
		templateSourceText.setLayoutData(gd);

		
		// Populate owner text field
		try {
			String templateSourceFolder =
				((IResource) getElement()).getPersistentProperty(
					new QualifiedName("", TEMPLATE_SOURCE_PROPERTY));
			templateSourceText.setText((templateSourceFolder != null) ? templateSourceFolder : DEFAULT_TEMPLATE_SOURCE);
		} catch (CoreException e) {
			templateSourceText.setText(DEFAULT_TEMPLATE_SOURCE);
		}
	}

	protected Control createContents(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		GridData data = new GridData(GridData.FILL);
		data.grabExcessHorizontalSpace = true;
		composite.setLayoutData(data);

		addFirstSection(composite);
		addSeparator(composite);
		addSecondSection(composite);
		return composite;
	}

	private Composite createDefaultComposite(Composite parent) {
		Composite composite = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		composite.setLayout(layout);

		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		composite.setLayoutData(data);

		return composite;
	}

	protected void performDefaults() {
	}
	
	public boolean performOk() {
		try {
			if (isJamonProjectCheckbox.getSelection()) {
				JamonNature.addToProject(getJavaProject().getProject());
			}
			else {
				JamonNature.removeFromProject(getJavaProject().getProject());
			}
		} catch(CoreException ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

}