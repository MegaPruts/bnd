package biz.aQute.resolve;

import static aQute.lib.exceptions.SupplierWithException.asSupplierOrElse;
import static java.util.stream.Collectors.toList;

import java.io.BufferedReader;
import java.io.File;
import java.util.Collections;
import java.util.function.Consumer;

import aQute.bnd.build.Project;
import aQute.bnd.build.Workspace;
import aQute.bnd.osgi.Constants;
import aQute.bnd.osgi.repository.AbstractIndexingRepository;
import aQute.bnd.osgi.resource.ResourceBuilder;
import aQute.lib.io.IO;

public class WorkspaceResourcesRepository extends AbstractIndexingRepository<Project>
	implements WorkspaceRepositoryMarker {
	private final Workspace		workspace;

	public WorkspaceResourcesRepository(Workspace workspace) {
		super();
		this.workspace = workspace;
		initialize();
	}

	private void initialize() {
		for (Project project : workspace.getAllProjects()) {
			File target = project.getTargetDir();
			File buildfiles = new File(target, Constants.BUILDFILES);
			if (buildfiles.isFile()) {
				index(project, asSupplierOrElse(() -> {
					try (BufferedReader rdr = IO.reader(buildfiles)) {
						return rdr.lines()
							.map(line -> IO.getFile(target, line.trim()))
							.filter(File::isFile)
							.collect(toList());
					}
				}, Collections.emptyList()));
			}
		}
	}

	@Override
	protected boolean isValid(Project project) {
		return workspace.isPresent(project.getName()) && project.isValid();
	}

	@Override
	protected Consumer<ResourceBuilder> customizer(Project project) {
		// Add a capability specific to the workspace so that we can
		// identify this fact later during resource processing.
		String name = project.getName();
		return rb -> rb.addWorkspaceNamespace(name);
	}

	@Override
	public String toString() {
		return NAME;
	}
}
