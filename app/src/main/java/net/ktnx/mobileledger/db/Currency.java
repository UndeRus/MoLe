package net.ktnx.mobileledger.db;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "currencies")
public class Currency {
    @PrimaryKey(autoGenerate = true)
    @NonNull
    private final Long id;
    @NonNull
    private String name;
    @NonNull
    private String position;
    @NonNull
    @ColumnInfo(name = "has_gap")
    private Boolean hasGap;
    public Currency(@NonNull Long id, @NonNull String name, @NonNull String position,
                    @NonNull Boolean hasGap) {
        this.id = id;
        this.name = name;
        this.position = position;
        this.hasGap = hasGap;
    }
    @NonNull
    public Long getId() {
        return id;
    }
    @NonNull
    public String getName() {
        return name;
    }
    public void setName(@NonNull String name) {
        this.name = name;
    }
    @NonNull
    public String getPosition() {
        return position;
    }
    public void setPosition(@NonNull String position) {
        this.position = position;
    }
    @NonNull
    public Boolean getHasGap() {
        return hasGap;
    }
    public void setHasGap(@NonNull Boolean hasGap) {
        this.hasGap = hasGap;
    }
}
