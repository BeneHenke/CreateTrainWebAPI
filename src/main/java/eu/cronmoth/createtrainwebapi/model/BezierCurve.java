package eu.cronmoth.createtrainwebapi.model;

import com.simibubi.create.content.trains.track.BezierConnection;
import net.minecraft.world.phys.Vec3;

public class BezierCurve {
    public PointData p0;
    public PointData p1;
    public PointData p2;
    public PointData p3;

    public BezierCurve(BezierConnection bezierConnection) {
        Vec3 start = bezierConnection.starts.getFirst();
        Vec3 end = bezierConnection.starts.getSecond();
        Vec3 startAxis = bezierConnection.axes.getFirst();
        Vec3 endAxis = bezierConnection.axes.getSecond();
        double handleLength = bezierConnection.getHandleLength();

        this.p0 = new PointData(start);
        this.p1 = new PointData(start.add(startAxis.scale(handleLength)));
        this.p2 = new PointData(end.add(endAxis.scale(handleLength)));
        this.p3 = new PointData(end);
    }
}
