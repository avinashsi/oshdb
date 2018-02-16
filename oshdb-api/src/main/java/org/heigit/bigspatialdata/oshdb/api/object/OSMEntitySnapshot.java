package org.heigit.bigspatialdata.oshdb.api.object;

import com.vividsolutions.jts.geom.Geometry;
import org.heigit.bigspatialdata.oshdb.util.OSHDBTimestamp;
import org.heigit.bigspatialdata.oshdb.osm.OSMEntity;
import org.heigit.bigspatialdata.oshdb.util.celliterator.CellIterator;
import org.heigit.bigspatialdata.oshdb.util.celliterator.LazyEvaluatedObject;

/**
 * Stores information about a single data entity at a specific time "snapshot".
 *
 * Alongside the entity and the timestamp, also the entity's geometry is provided.
 */
public class OSMEntitySnapshot implements OSHDBMapReducible {
  private final CellIterator.IterateByTimestampEntry data;
  
  public OSMEntitySnapshot(CellIterator.IterateByTimestampEntry data) {
    this.data = data;
  }

  /**
   * The timestamp for which the snapshot of this data entity has been obtained.
   *
   * @return snapshot timestamp as an OSHDBTimestamp object
   */
  public OSHDBTimestamp getTimestamp() {
    return data.timestamp;
  }

  /**
   * The geometry of this entity at the snapshot's timestamp clipped to the requested area of
   * interest.
   *
   * @return the geometry as a JTS Geometry
   */
  public Geometry getGeometry() {
    return data.geometry.get();
  }

  /**
   * The geometry of this entity at the snapshot's timestamp. This is the full (unclipped) geometry
   * of the osm entity.
   *
   * @return the unclipped geometry of the osm entity snapshot as a JTS Geometry
   */
  public Geometry getGeometryUnclipped() {
    return data.unclippedGeometry.get();
  }

  /**
   * The entity for which the snapshot has been obtained.
   *
   * This is the (not deleted) version of a OSHEntity that was valid at the provided snapshot timestamp.
   *
   * @return the OSMEntity object of this snapshot
   */
  public OSMEntity getEntity() {
    return data.osmEntity;
  }
}
