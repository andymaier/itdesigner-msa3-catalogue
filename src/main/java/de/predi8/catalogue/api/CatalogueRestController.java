package de.predi8.catalogue.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.predi8.catalogue.error.NotFoundException;
import de.predi8.catalogue.event.NullAwareBeanUtilsBean;
import de.predi8.catalogue.event.Operation;
import de.predi8.catalogue.model.Article;
import de.predi8.catalogue.repository.ArticleRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/articles")
public class CatalogueRestController {

	public static final String PRICE = "price";
	public static final String NAME = "name";
	private ArticleRepository repo;
	private KafkaTemplate<String, Operation> kafka;
	final private ObjectMapper mapper;
	NullAwareBeanUtilsBean beanUtils;

	public CatalogueRestController(ArticleRepository repo, KafkaTemplate<String, Operation> kafka, ObjectMapper mapper, NullAwareBeanUtilsBean beanUtils) {
		this.repo = repo;
		this.kafka = kafka;
		this.mapper = mapper;
		this.beanUtils = beanUtils;
	}

	@GetMapping
	public List<Article> index() {
		return repo.findAll();
	}

	@GetMapping("/count")
	public long count() {
		return repo.count();
	}

	public Article get(String id) {
		return repo.findById(id).orElseThrow(NotFoundException::new);
	}

	@GetMapping("/{id}")
	public Article getById(@PathVariable String id) {
		return get(id);
	}

	@PostMapping
	public ResponseEntity<Article> create(@RequestBody Article article, UriComponentsBuilder builder) {
		String uuid = UUID.randomUUID().toString();
		article.setUuid(uuid);

		System.out.println("article = " + article);

		Article save = repo.save(article);

		return ResponseEntity.created(builder.path("/articles/" + uuid).build().toUri()).body(save);
	}

	@DeleteMapping("/{id}")
	public void deleteArticle(@PathVariable String id) {
		Article article = get(id);
		repo.delete(article);
	}

	@PutMapping("/{id}")
	public void change(@PathVariable String id, @RequestBody Article article) {
		get(id);
		article.setUuid(id);
		repo.save(article);
	}

	@PatchMapping("/{id}")
	public ResponseEntity<Article> patch(@PathVariable String id, @RequestBody JsonNode json, UriComponentsBuilder builder) {
		Article old = get(id);

		if(json.has(PRICE)) {
			if(json.hasNonNull(PRICE)) {
				old.setPrice(new BigDecimal(json.get(PRICE).asDouble()));
			}
		}
		if(json.has(NAME)) {
			old.setName(json.get(NAME).asText());
		}

		Article save = repo.save(old);

		return ResponseEntity.ok(save);
	}
}