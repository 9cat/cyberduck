package ch.cyberduck.ui.resources;

import ch.cyberduck.core.Protocol;
import ch.cyberduck.core.local.Local;

/**
 * @version $Id:$
 */
public abstract class AbstractIconCache<I> implements IconCache<I> {

    /**
     * @param name Icon name
     * @return Cached image
     */
    @Override
    public I iconNamed(final String name) {
        return this.iconNamed(name, null);
    }

    /**
     * @param name Icon filename with extension
     * @param size Requested size
     * @return Cached image
     */
    @Override
    public I iconNamed(final String name, final Integer size) {
        return this.iconNamed(name, size, size);
    }

    @Override
    public I volumeIcon(final Protocol protocol, final Integer size) {
        return this.iconNamed(protocol.disk(), size);
    }

    @Override
    public I fileIcon(final Local item, final Integer size) {
        return this.documentIcon(item.getExtension(), size);
    }

    @Override
    public I aliasIcon(final String extension, final Integer size) {
        return this.badge(this.iconNamed("aliasbadge.tiff", size), this.documentIcon(extension, size));
    }

    protected abstract I badge(final I badge, final I icon);
}
