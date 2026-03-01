package br.com.mypersonalmoney.category;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/categories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CategoryResource {

    private final CategoryService service;

    public CategoryResource(CategoryService service) {
        this.service = service;
    }

    public record CategoryResponse(Long id, String name, CategoryType type, boolean active) {
        static CategoryResponse from(Category c) {
            return new CategoryResponse(c.id, c.name, c.type, c.active);
        }
    }

    public record CreateCategoryRequest(String name, CategoryType type) {}
    public record UpdateCategoryRequest(String name, Boolean active) {}

    @GET
    public List<CategoryResponse> list(@QueryParam("type") CategoryType type,
                                       @QueryParam("active") Boolean active) {
        return service.listCategories(type, active)
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @GET
    @Path("/{id}")
    public CategoryResponse get(@PathParam("id") Long id) {
        Category c = service.getCategory(id);
        if (c == null) throw new NotFoundException("category not found");
        return CategoryResponse.from(c);
    }

    @POST
    public Response create(CreateCategoryRequest req) {
        if (req == null) throw new BadRequestException("body is required");

        try {
            Category c = service.createCategory(req.name(), req.type());
            return Response.status(Response.Status.CREATED)
                    .entity(CategoryResponse.from(c))
                    .build();
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            // unique index name+type
            throw new WebApplicationException("category already exists (name+type)", 409);
        }
    }

    @PATCH
    @Path("/{id}")
    public CategoryResponse update(@PathParam("id") Long id, UpdateCategoryRequest req) {
        if (req == null) throw new BadRequestException("body is required");

        try {
            Category c = service.updateCategory(id, req.name(), req.active());
            return CategoryResponse.from(c);
        } catch (IllegalArgumentException e) {
            if ("category not found".equals(e.getMessage())) {
                throw new NotFoundException(e.getMessage());
            }
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            throw new WebApplicationException("category already exists (name+type)", 409);
        }
    }
}