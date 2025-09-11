package io.github.ckofa.filewatcher.model;

/**
 * Defines a contract for creating a standard prefix for messages.
 * Implementations of this interface can provide various formats for message prefixes,
 * for example, by including timestamps or application-specific identifiers.
 */
public interface MessagePrefixProvider {

    String getMessagePrefix();
}
