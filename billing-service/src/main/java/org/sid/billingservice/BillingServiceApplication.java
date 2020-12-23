package org.sid.billingservice;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import net.minidev.json.annotate.JsonIgnore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.*;
import java.util.Collection;
import java.util.Date;

@Entity
@Data @AllArgsConstructor @NoArgsConstructor @ToString
class Bill {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	private Date billingDate;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long customerID;
	@Transient
	private Customer customer;
	@OneToMany(mappedBy = "bill")
	private Collection<ProductItem> productItems;
}

@Entity
@Data @AllArgsConstructor @NoArgsConstructor @ToString
class ProductItem {
	@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Long productID;
	private double price;
	private double quantity;
	@Transient
	private Product product;
	@ManyToOne
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // @JsonIgnore ignore le (Write et le Read), alors que avec @JsonProperty on peut controller de plus la seriallisation en Json
	private Bill bill;
}

@Projection(name = "fullBill", types = Bill.class)
interface BillProjection {
	public Long getId();
	public Date getBillingDate();
	public Long getCustomerID();
	public Collection<ProductItem> getProductItems();

}
@Data
class Customer {
	private Long id; private String name; private String email;
}

@FeignClient(name = "CUSTOMER-SERVICE") //ici spring data ne sert à rien car Customer n'est pas une entity, alors on utilise l'interface "Open Feign" qui est équivalante à spring data mais pour les API REST
interface CustomerService {
	@GetMapping("/customers/{id}")
	public Customer findCustomerById(@PathVariable(name = "id") Long id);
}

@Data
class Product {
	private Long id; private String name; private double price;
}
@FeignClient(name = "INVENTORY-SERVICE")
interface ProductService {
	@GetMapping("/products/{id}")
	public Product findProductById(@PathVariable(name = "id") Long id);
	@GetMapping("/products")
	public PagedModel<Product> findAllProducts();
}

@RepositoryRestResource
interface BillRepository extends JpaRepository<Bill, Long>{}

@RepositoryRestResource
interface ProductItemRepository extends JpaRepository<ProductItem,Long>{}

@RestController
class BillRestController {
	@Autowired
	private BillRepository billRepository;
	@Autowired
	private ProductItemRepository productItemRepository;
	@Autowired
	private CustomerService customerService;
	@Autowired
	private ProductService productService;

	@GetMapping("/fullBill/{id}")
	public Bill getBill(@PathVariable(name = "id") Long id){
		Bill bill = billRepository.findById(id).get();
		bill.setCustomer(customerService.findCustomerById(bill.getCustomerID()));
		bill.getProductItems().forEach(productItem -> {
			productItem.setProduct(productService.findProductById(productItem.getProductID()));
		});
		return bill;
	}


}

@SpringBootApplication
@EnableFeignClients //à ne pas oublier de activer Feign Client par cette annotation
public class BillingServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillingServiceApplication.class, args);
	}

	@Bean
	CommandLineRunner start(BillRepository billRepository, ProductItemRepository productItemRepository, CustomerService customerService, ProductService productService) {
		return args -> {
			Customer c1 = customerService.findCustomerById(1L);
			System.out.println("*********************");
			System.out.println("ID :" + c1.getId());
			System.out.println("Name :" + c1.getName());
			System.out.println("Email :" + c1.getEmail());
			System.out.println("*********************");
			Bill bill1 = billRepository.save(new Bill(null, new Date(), c1.getId(), null, null));
			Bill bill2 = billRepository.save(new Bill(null, new Date(), c1.getId(), null, null));

			PagedModel<Product> products = productService.findAllProducts();
			products.getContent().forEach(product -> {
				productItemRepository.save(new ProductItem(null, product.getId(), product.getPrice(), 23.00, null, bill1));
				productItemRepository.save(new ProductItem(null, product.getId(), product.getPrice(), 23.00, null, bill2));

			});

		};
	}
}
