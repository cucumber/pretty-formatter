package io.cucumber.prettyformatter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

public class UriUtils {
    static URI relativize(String uri) {
        return relativize(URI.create(uri));
    }

    static URI relativize(URI uri) {
        // TODO: Needed?
        // TODO: What should urls in reports looks like? Relative to working directory?
        if (!"file".equals(uri.getScheme())) {
            return uri;
        }
        if (!uri.isAbsolute()) {
            return uri;
        }

        try {
            URI root = new File("").toURI();
            URI relative = root.relativize(uri);
            // Scheme is lost by relativize
            return new URI("file", relative.getSchemeSpecificPart(), relative.getFragment());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
}
