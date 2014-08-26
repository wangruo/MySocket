package FirstExercise;

public abstract class ServiceBase implements IService {

    private boolean running = false;

    protected boolean isRunning() {
        return running;
    }

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }
}
