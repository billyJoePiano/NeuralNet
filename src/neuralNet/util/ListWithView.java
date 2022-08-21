package neuralNet.util;

import java.util.*;

public class ListWithView<T> {
    public final List<T> inputs;
    public final List<T> view;

    public ListWithView(final List<T> inputs) {
        this.inputs = inputs;
        this.view = Collections.unmodifiableList(inputs);
    }

    public ListWithView(final List<T> inputs, final List<T> view) {
        this.inputs = inputs;
        this.view = view;
    }
}
