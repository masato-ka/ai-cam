package ka.masato.aithermalcam.model;

public class ObjectInfo {

    public ObjectInfo(String objectName, float temperature) {
        this.objectName = objectName;
        this.temperature = temperature;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    private String objectName;
    private float temperature;

}
