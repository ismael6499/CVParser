package datasets;

import java.util.Objects;

public class NameObject {
    private  boolean creadoDefault = false;
    private String data;
    private long frequency;
    private long total = 1;

    public NameObject(String data, long frequency) {
        this.data = data;
        this.frequency = frequency;
    }

    public NameObject(String data, long frequency,boolean creadoDefault) {
        this.data = data;
        this.frequency = frequency;
        this.creadoDefault = creadoDefault;
    }
    public NameObject(String data, String frequency) {
        this.data = data;
        try {
            this.frequency = Long.parseLong(frequency);
        } catch (NumberFormatException ignored) {
        }
    }

    public long getTotalFrequency(){
        return total + frequency;
    }
    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public String getData() {
        return data;
    }

    public boolean isCreadoDefault() {
        return creadoDefault;
    }

    public void setCreadoDefault(boolean creadoDefault) {
        this.creadoDefault = creadoDefault;
    }

    public long getFrequency() {
        return frequency;
    }


    public void setFrequency(long frequency) {
        this.frequency = frequency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NameObject that = (NameObject) o;
        return Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data);
    }
}
