package org.javagram.dao.proxy.changes;

/**
 * Created by HerrSergio on 21.05.2016.
 */
public abstract class LazyObject<T> {

    private boolean gotten;
    private T object;

    protected abstract T get();

    public boolean isGotten() {
        return gotten;
    }

    public T getObject() {
        if(!isGotten())
            update();

        return object;
    }

    public void update() {
        object = get();
        gotten = true;
    }
}
