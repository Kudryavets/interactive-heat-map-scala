package observatory

import com.sksamuel.scrimage.{Image, Pixel}

import scala.math._

/**
  * 2nd milestone: basic visualization
  */
object Visualization {
  val DEFAULT_POWER_PARAMETER = 2D

  /**
    * @param temperatures Known temperatures: pairs containing a location and the temperature at this location
    * @param location Location where to predict the temperature
    * @return The predicted temperature at `location`
    */
  def predictTemperature(temperatures: Iterable[(Location, Double)], location: Location): Double =
    temperatures.find{ case (loc, _) => loc == location }.map{ case (_, temp) => temp}
      .getOrElse(predictTemperatureImpl(temperatures, location, DEFAULT_POWER_PARAMETER))

  def predictTemperatureImpl(temperatures: Iterable[(Location, Double)],
                             location: Location,
                             p: Double): Double = {
    val (sumWeightedTemps, sumWeghts) = temperatures
      .map{ case (knownLoc, temp) => (temp, 1 / pow(distance(loc2Rad(knownLoc), loc2Rad(location)), p))}
      .aggregate(0D, 0D) (
        (acc, tempWeigtPair) => (acc._1 + tempWeigtPair._1 * tempWeigtPair._2, acc._2 + tempWeigtPair._2),
        (acc1, acc2) => (acc1._1 + acc2._1, acc1._2 + acc2._2)
      )

    sumWeightedTemps / sumWeghts
  }

  def distance(loc1: Location, loc2: Location): Double = {
    val delta = abs(loc1.lon - loc2.lon)
    val cosDelta = cos(delta)
    val cosLat1 = cos(loc1.lat)
    val cosLat2 = cos(loc2.lat)
    val sinLat1 = sin(loc1.lat)
    val sinLat2 = sin(loc2.lat)

    atan2(
      sqrt(pow(cosLat2*sin(delta), 2) + pow(cosLat1*sinLat2 - sinLat1*cosLat2*cosDelta, 2)),
      sinLat1*sinLat2 + cosLat1*cosLat2*cosDelta
    )
  }

  def loc2Rad(loc: Location): Location = Location(toRadians(loc.lat), toRadians(loc.lon))


  /**
    * @param points Pairs containing a value and its associated color
    * @param value The value to interpolate
    * @return The color that corresponds to `value`, according to the color scale defined by `points`
    */
  def interpolateColor(points: Iterable[(Double, Color)], value: Double): Color = interpolateColorRec(points.toList.sortBy(- _._1), value)

  private def interpolateColorRec(points: List[(Double, Color)], value: Double): Color = {
    val first :: second :: tail = points

    if (value >= first._1) {
      first._2
    } else if (first._1 > value && value > second._1) {
      val ratio = (first._1 - value) / (first._1 - second._1)
      Color(
        iterpolate(first._2.red, second._2.red, ratio),
        iterpolate(first._2.green, second._2.green, ratio),
        iterpolate(first._2.blue, second._2.blue, ratio)
      )
    } else if (tail.nonEmpty) {
      interpolateColor(points.tail, value)
    } else {
      second._2
    }
  }

  private def iterpolate(x0: Int, x1:Int, ratio: Double): Int = math.round((1 - ratio) * x0 + ratio * x1).toInt

  /**
    * @param temperatures Known temperatures
    * @param colors Color scale
    * @return A 360×180 image where each pixel shows the predicted temperature at its location
    */
  def visualize(temperatures: Iterable[(Location, Double)], colors: Iterable[(Double, Color)]): Image = {
    val pixelArray: Array[Pixel] = new Array[Pixel](360*180)
    val colorsList = colors.toList
    val temperaturesVec = temperatures.toVector

    ( for { x <- 0 to 359; y <- 0 to 179 } yield (x, y) ).toVector.par
      .foreach { case (x, y) =>
        val loc = Location(90 - y, x - 180)
        val temp = predictTemperature(temperaturesVec, loc)
        val color = interpolateColor(colorsList, temp)
        val pixel = Pixel(color.red, color.green, color.blue, 127)
        pixelArray(y * 360 + x) = pixel
      }

    Image(360, 180, pixelArray)
  }
}
