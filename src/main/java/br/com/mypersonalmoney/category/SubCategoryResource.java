package br.com.mypersonalmoney.category;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/subcategories")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SubCategoryResource {

    private final CategoryService service;

    public SubCategoryResource(CategoryService service) {
        this.service = service;
    }

    public record SubCategoryResponse(Long id, Long categoryId, String name, boolean active) {}

    public record CreateSubCategoryRequest(Long categoryId, String name) {}
    public record UpdateSubCategoryRequest(String name, Boolean active) {}

    @GET
    public List<SubCategoryResponse> list(@QueryParam("categoryId") Long categoryId,
                                          @QueryParam("active") Boolean active) {

        // Single query projection: no lazy, no N+1
        StringBuilder jpql = new StringBuilder(
                "select s.id, s.category.id, s.name, s.active from SubCategory s where 1=1"
        );

        if (categoryId != null) jpql.append(" and s.category.id = :categoryId");
        if (active != null) jpql.append(" and s.active = :active");
        jpql.append(" order by s.name");

        var q = PanacheEntityBase.getEntityManager().createQuery(jpql.toString(), Object[].class);

        if (categoryId != null) q.setParameter("categoryId", categoryId);
        if (active != null) q.setParameter("active", active);

        return q.getResultList()
                .stream()
                .map(r -> new SubCategoryResponse(
                        (Long) r[0],
                        (Long) r[1],
                        (String) r[2],
                        (Boolean) r[3]
                ))
                .toList();
    }

    @GET
    @Path("/{id}")
    public SubCategoryResponse get(@PathParam("id") Long id) {
        Object[] r = PanacheEntityBase.getEntityManager()
                .createQuery(
                        "select s.id, s.category.id, s.name, s.active from SubCategory s where s.id = :id",
                        Object[].class
                )
                .setParameter("id", id)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (r == null) throw new NotFoundException("subcategory not found");

        return new SubCategoryResponse((Long) r[0], (Long) r[1], (String) r[2], (Boolean) r[3]);
    }

    @POST
    public Response create(CreateSubCategoryRequest req) {
        if (req == null) throw new BadRequestException("body is required");

        try {
            SubCategory created = service.createSubCategory(req.categoryId(), req.name());
            // sem lazy: categoryId vem do request
            return Response.status(Response.Status.CREATED)
                    .entity(new SubCategoryResponse(created.id, req.categoryId(), created.name, created.active))
                    .build();
        } catch (IllegalArgumentException e) {
            if ("category not found".equals(e.getMessage())) throw new NotFoundException(e.getMessage());
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            throw new WebApplicationException("subcategory already exists in this category", 409);
        }
    }

    @PATCH
    @Path("/{id}")
    public SubCategoryResponse update(@PathParam("id") Long id, UpdateSubCategoryRequest req) {
        if (req == null) throw new BadRequestException("body is required");

        try {
            CategoryService.SubCategoryResult r = service.updateSubCategory(id, req.name(), req.active());
            return new SubCategoryResponse(r.id(), r.categoryId(), r.name(), r.active());
        } catch (IllegalArgumentException e) {
            if ("subcategory not found".equals(e.getMessage())) throw new NotFoundException(e.getMessage());
            throw new BadRequestException(e.getMessage());
        } catch (Exception e) {
            throw new WebApplicationException("subcategory already exists in this category", 409);
        }
    }
}