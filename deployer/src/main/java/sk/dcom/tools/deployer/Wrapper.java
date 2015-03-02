package sk.dcom.tools.deployer;

/**
 * @author <a href="mailto:patrikbeno@gmail.com">Patrik Beno</a>
 */
public class Wrapper<T> {

	T object;

	public Wrapper() {
	}

	public Wrapper(T object) {
		this.object = object;
	}

	public T getObject() {
		return object;
	}

	public void setObject(T object) {
		this.object = object;
	}
}
