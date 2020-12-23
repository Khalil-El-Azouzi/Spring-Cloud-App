package org.sid.gatewayservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.gateway.discovery.DiscoveryClientRouteDefinitionLocator;
import org.springframework.cloud.gateway.discovery.DiscoveryLocatorProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@SpringBootApplication
@EnableHystrix // on active Hystrix pour bien utiliser son service de DashBoard qui serveille l'état du trafic au niveau du service Gateway
public class GatewayServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayServiceApplication.class, args);
	}

	// 3éme solution en utilisant une classe de configuration
	// pour configurer les routes statiquement
	@Bean
	RouteLocator getStaticRoutes(RouteLocatorBuilder routeLocatorBuilder){
		return routeLocatorBuilder.routes()
				// Si on a pas utiliser un service d'enregistrement (comme Eureka) on fait :
				//.route( r -> r.path("/customers/**").uri("http://localhost:8081/").id("r1"))
				//.route( r -> r.path("/products/**").uri("http://localhost:8082/").id("r2"))
				//Si on a configurer le service d'enregistrement Eureka on fait le path static on utilisan le load balancer :
				//.route( r -> r.path("/customers/**").uri("lb://CUSTOMER-SERVICE").id("r1"))
				//.route( r -> r.path("/products/**").uri("lb://INVENTORY-SERVICE").id("r2"))
				/*
				 *  "lb" est le load balancer
				 * c'est la répartition de charge qui désigne le processus de répartition d’un ensemble de tâches
				 *  sur un ensemble de ressources, dans le but d’en rendre le traitement global plus efficace.
				 * */
				//On utilise le site "RapidAPI" qui offre des api public. voila ça config :
				.route( r -> r
						.path("/publicCountries/**")
						//il faut ajouter un middleware "Filter" qui contient lui même une serie de filters pour permetre l'accée à l'api public
						.filters(f -> f
								.addRequestHeader("x-rapidapi-host", "restcountries-v1.p.rapidapi.com")
								.addRequestHeader("x-rapidapi-key", "7c048b5101mshaedff792b3d70a2p1449f1jsnf9c4d1b2c413")
								.rewritePath("/publicCountries/(?<segment>.*)","/${segment}") // il faut ajouter cet paramétre pour organiser les paths
								.hystrix(h -> h.setName("countries").setFallbackUri("forward:/defaultCountries"))
						)
						.uri("https://restcountries-v1.p.rapidapi.com/all")
						.id("countries")
				)
				.route( r -> r
				.path("/muslimSalat/**")
					//il faut ajouter un middleware "Filter" qui contient lui même une serie de filters pour permetre l'accée à l'api public
					.filters(f -> f
							.addRequestHeader("x-rapidapi-host", "muslimsalat.p.rapidapi.com")
							.addRequestHeader("x-rapidapi-key", "7c048b5101mshaedff792b3d70a2p1449f1jsnf9c4d1b2c413")
							.rewritePath("/muslimSalat/(?<segment>.*)","/${segment}") // il faut ajouter cet paramétre pour organiser les paths
							.hystrix(h->h.setName("muslimsalat").setFallbackUri("forward:/defaultMuslimPrayer"))
					)
					.uri("https://muslimsalat.p.rapidapi.com")
					.id("prayers")
				)
				.build();

	}

	//Pour configurer les routes dynamiquement c-à-d le nom du micro-service va être mentionner dans l'URI
	@Bean
	DiscoveryClientRouteDefinitionLocator getDynamicRoutes(ReactiveDiscoveryClient rdc, DiscoveryLocatorProperties dlp) {
		return new DiscoveryClientRouteDefinitionLocator(rdc, dlp);
	}
}

@RestController
class CircuitBreakerRestController{
	@GetMapping("/defaultCountries")
	public Map<String,String> countries(){
		Map<String,String> data = new HashMap<>();
		data.put("message","default Countries");
		data.put("countries","Morocco, Algeria, Tunis, ....");
		return data;
	}

	@GetMapping("/defaultMuslimPrayer")
	public Map<String,String> muslimPrayer(){
		Map<String,String> map = new HashMap<>();
		map.put("message","default Muslim Prayer Fallback");
		map.put("country","Morocco");
		map.put("city", "Casablanca");
		map.put("fajr","5:38 am");
		map.put("shurooq", "7:00 am");
		map.put("dhuhr", "1:31 pm");
		map.put("asr", "5:08 pm");
		map.put("maghrib", "8:02 pm");
		map.put("isha", "9:19 pm");
		return map;
	}
}


