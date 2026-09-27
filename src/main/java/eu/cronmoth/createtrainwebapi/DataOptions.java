package eu.cronmoth.createtrainwebapi;

/**
 * Which optional data the API collects and returns. Disabled groups are not computed and appear as
 * {@code null} in the JSON; {@code /features} reports this record so clients can hide what's missing.
 * Positions, tracks and stations are always included.
 *
 * @param owner          train owner uuid and name
 * @param trainStatus    speed, max speed, derailed
 * @param passengers     number of players on board
 * @param navigation     destination, distance to it, waiting at a signal
 * @param schedule       schedule title, current stop, state, wait reason
 * @param route          planned route (node pairs) to the destination
 * @param stationArrivals per station: train at the platform, arriving train, next arrivals
 * @param signals        signals, their states, signal block sections and occupied blocks
 * @param portals        node pairs linked through portals
 * @param cargo          the /cargo endpoint
 */
public record DataOptions(
        boolean owner,
        boolean trainStatus,
        boolean passengers,
        boolean navigation,
        boolean schedule,
        boolean route,
        boolean stationArrivals,
        boolean signals,
        boolean portals,
        boolean cargo
) {
    /** Nothing optional; used until the config has been read. */
    public static final DataOptions NONE = new DataOptions(false, false, false, false, false, false, false, false, false, false);
}
