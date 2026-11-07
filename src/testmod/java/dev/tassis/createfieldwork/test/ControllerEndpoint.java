package dev.tassis.createfieldwork.test;

/** Provenance attached to the real upstream temporary endpoint, test-only. */
public interface ControllerEndpoint {
    ProbeController.Source probe$source();
    void probe$capture(ProbeController.Source source);
    String probe$publishedScope();
    void probe$publishedScope(String value);
    boolean probe$restrictedHistory();
    void probe$markRestricted();
    void probe$restricted(boolean value);
}
