package sk.dcom.tools.deployer;

import org.apache.commons.lang3.StringUtils;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
public class MvnUri {

	String uri;

	String groupId;

	String artifactId;

	String version;

	String classifier;

	String type;

	String resolvedSnapshotVersion;

	static public MvnUri parse(String uri) {

		MvnUri mvnuri = new MvnUri();
		mvnuri.uri = uri;

		String[] parts = uri.split(":");
		mvnuri.groupId = parts[0];
		mvnuri.artifactId = parts[1];
		mvnuri.version = parts[2];
		mvnuri.classifier = (parts.length > 4) ? parts[4] : null;
		mvnuri.type = parts[3];

		return mvnuri;
	}

	protected MvnUri() {
	}

	public MvnUri(MvnUri src) {
		this.groupId = src.groupId;
		this.artifactId = src.artifactId;
		this.version = src.version;
		this.classifier = src.classifier;
		this.type = src.type;
		this.resolvedSnapshotVersion = src.resolvedSnapshotVersion;
		this.uri = asString();
	}

	public boolean isSnapshot() {
		return version.endsWith("-SNAPSHOT");
	}

	public String asString() {
		return (classifier != null)
				? String.format("%s:%s:%s:%s:%s", groupId, artifactId, version, type, classifier)
				: String.format("%s:%s:%s:%s", groupId, artifactId, version, type);
	}

	public String getPath() {
		String sversion = StringUtils.defaultIfEmpty(resolvedSnapshotVersion, version);
		String path = (classifier != null)
				? String.format("%1$s/%2$s/%3$s/%2$s-%4$s-%5$s.%6$s",
									 groupId.replace('.', '/'), artifactId, version, sversion, classifier, type)
				: String.format("%1$s/%2$s/%3$s/%2$s-%4$s.%5$s",
									 groupId.replace('.', '/'), artifactId, version, sversion, type);
		return path;

	}

	public MvnUri clone(String type, String classifier) {
		MvnUri copy = new MvnUri(this);
		copy.type = type;
		copy.classifier = classifier;
		return copy;
	}


	@Override
	public String toString() {
		return asString();
	}
}
