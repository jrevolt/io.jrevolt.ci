package sk.dcom.tools.deployer;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 * @version $Id$
 */
public class DeploymentException extends RuntimeException {
	public DeploymentException(String message, Throwable cause) {
		super(message, cause);
	}

	public DeploymentException(Throwable cause) {
		super(cause);
	}
}
