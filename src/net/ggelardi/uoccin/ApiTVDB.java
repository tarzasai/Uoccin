package net.ggelardi.uoccin;

import retrofit.http.GET;
import retrofit.http.Path;

public class ApiTVDB {
	private static final String API_URL = "http://thetvdb.com/api";
	private static final String API_KEY = "A74D017DA5F2C3B0";
	
	interface TVDB {
		
		@GET("/series/{tvdb_id}/{language}.xml")
		void getSeries(@Path("tvdb_id") String tvdb_id, @Path("language") String language);
		
	}
	
	static class Series {
		int id;
	}
	
}

/*

<?xml version="1.0" encoding="UTF-8" ?>
<Data>
  <Series>
    <id>123581</id>
    <Actors>|Matt LeBlanc|Tamsin Greig|Stephen Mangan|Kathleen Rose Perkins|John Pankow|Mircea Monroe|</Actors>
    <Airs_DayOfWeek>Sunday</Airs_DayOfWeek>
    <Airs_Time>10:30 PM</Airs_Time>
    <ContentRating>TV-MA</ContentRating>
    <FirstAired>2011-01-09</FirstAired>
    <Genre>|Comedy|</Genre>
    <IMDB_ID>tt1582350</IMDB_ID>
    <Language>en</Language>
    <Network>Showtime</Network>
    <NetworkID></NetworkID>
    <Overview>Episodes follows the lives of Sean and Beverly Lincoln, British sitcom producers who are persuaded to move to Hollywood and remake their series for an American audience. When the couple is forced to cast Matt LeBlanc in the lead role, a bizarre triangle is formed that strains their marriage and threatens their show.</Overview>
    <Rating>7.7</Rating>
    <RatingCount>39</RatingCount>
    <Runtime>30</Runtime>
    <SeriesID>78049</SeriesID>
    <SeriesName>Episodes</SeriesName>
    <Status>Continuing</Status>
    <added>2009-11-10 05:59:31</added>
    <addedBy>76261</addedBy>
    <banner>graphical/123581-g5.jpg</banner>
    <fanart>fanart/original/123581-5.jpg</fanart>
    <lastupdated>1422178378</lastupdated>
    <poster>posters/123581-7.jpg</poster>
    <tms_wanted_old>1</tms_wanted_old>
    <zap2it_id>EP01352131</zap2it_id>
  </Series>
</Data>

*/