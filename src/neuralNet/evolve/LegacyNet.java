package neuralNet.evolve;

import neuralNet.network.*;

public class LegacyNet<N extends NeuralNet<?, N, ?>> {
    public final N net;



    public LegacyNet(N net) {
        this.net = net;
    }


}
