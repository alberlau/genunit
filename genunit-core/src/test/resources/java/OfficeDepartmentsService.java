package com.accuity.data.service.department;

import static com.accuity.data.service.department.RelationshipHelper.getRelationships;
import static com.accuity.data.util.SearchUtils.getSearchParamsByIds;

import com.accuity.data.domain.department.DepartmentPosition;
import com.accuity.data.domain.department.OfficeDepartment;
import com.accuity.data.domain.department.OfficeDepartments;
import com.accuity.data.domain.relationship.Relationships;
import com.accuity.data.domain.task.TaskType;
import com.accuity.data.exception.EntityNotFoundException;
import com.accuity.data.rest.client.HeraClient;
import com.accuity.data.service.async.HeraAsyncService;
import com.accuity.data.service.bulk.HeraBulkService;
import com.accuity.data.service.person.PersonService;
import com.accuity.data.service.relationship.RelationshipService;
import com.accuity.data.util.StrUtils;
import com.accuity.domain.AbstractSerializableRoot;
import com.accuity.domain.legalEntity.LegalEntity;
import com.accuity.domain.office.Office;
import com.accuity.domain.person.Person;
import com.accuity.domain.relationship.Relationship;
import com.accuity.domain.relationship.RelationshipParty;
import com.accuity.domain.task.Task;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;

@Service
public class OfficeDepartmentsService {
    private final DepartmentService departmentService;
    private final DepartmentUpdateService departmentUpdateService;
    private final OtherPositionService otherPositionService;
    private final OtherPositionUpdateService otherPositionUpdateService;
    private final RelationshipService relationshipService;
    private final HeraBulkService heraBulkService;
    private final HeraClient heraClient;
    private final PersonService personService;
    private final HeraAsyncService heraAsyncService;

    public OfficeDepartmentsService(DepartmentService departmentService,
        DepartmentUpdateService departmentUpdateService,
        OtherPositionService otherPositionService,
        OtherPositionUpdateService otherPositionUpdateService,
        RelationshipService relationshipService,
        HeraBulkService heraBulkService,
        HeraClient heraClient,
        PersonService personService, HeraAsyncService heraAsyncService) {
        this.departmentService = departmentService;
        this.departmentUpdateService = departmentUpdateService;
        this.otherPositionService = otherPositionService;
        this.otherPositionUpdateService = otherPositionUpdateService;
        this.relationshipService = relationshipService;
        this.heraBulkService = heraBulkService;
        this.heraClient = heraClient;
        this.personService = personService;
        this.heraAsyncService = heraAsyncService;
    }

    public OfficeDepartments getOfficeDepartments(String officeId) {
        Relationships relationships = getRelationships(officeId, relationshipService);
        List<OfficeDepartment> officeDepartments = departmentService.getOfficeDepartments(officeId, relationships);
        List<DepartmentPosition> otherDepartmentPositions = otherPositionService.getOtherDepartmentPositions(officeId,
            relationships);
        return new OfficeDepartments(officeDepartments, otherDepartmentPositions);
    }

    public Task updateDepartmentsAndOtherPositions(String officeId, OfficeDepartments officeDepartments) {

        LegalEntity legalEntity = getLegalEntityFromOffice(officeId);

        List<AbstractSerializableRoot> created = new ArrayList<>();
        List<AbstractSerializableRoot> updated = new ArrayList<>();
        ArrayList<String> deletedDepartments = new ArrayList<>();

        Map<String, Relationship> allRelationships = new HashMap<>();

        departmentUpdateService.gahterRelationshipsForUpdate(officeDepartments.getDepartments(), allRelationships);
        otherPositionUpdateService.gatherRelationshipsForUpdate(officeDepartments.getOtherPositions(), allRelationships);

        departmentUpdateService
            .updateDepartmentList(legalEntity, officeId, officeDepartments.getDepartments(), allRelationships, created,
                updated, deletedDepartments);
        otherPositionUpdateService.updatePositionsList(legalEntity, officeId, officeDepartments.getOtherPositions(), allRelationships, created);

        updateEntities(created, updated, allRelationships);

        if (!deletedDepartments.isEmpty()) {
            return heraAsyncService.postAsyncRefs(TaskType.DEPARTMENT_DELETE, deletedDepartments);
        }
        return null;
    }

    private void updateEntities(List<AbstractSerializableRoot> created, List<AbstractSerializableRoot> updated,
        Map<String, Relationship> allRelationships) {
        List<AbstractSerializableRoot> deleted = new ArrayList<>();
        Set<String> deletePersonIds = new HashSet<>();
        for (Map.Entry<String, Relationship> relationship : allRelationships.entrySet()) {
            if (relationship.getValue().getId() == null) {
                created.add(relationship.getValue());
            } else {
                relationshipService.prepareEntityBeforeSave(relationship.getValue());
                if (relationshipHasPositions(relationship.getValue())) {
                    updated.add(relationship.getValue());
                } else {
                    deleted.add(relationship.getValue());
                    deletePersonIds.add(getRelationshipPersonId(relationship.getValue()));
                }
            }
        }
        addDeletedRelationshipPersons(deletePersonIds, deleted);
        heraBulkService.bulkUpdate(created, updated, deleted);
    }

    private String getRelationshipPersonId(Relationship relationship) {
        Optional<RelationshipParty> personParty = relationship.getParties().stream()
            .filter(party -> party.getEntityType().equals(Person.ENTITY_NAME))
            .findFirst();
        if (personParty.isPresent()) {
            return StrUtils.hrefToId(personParty.get().getEntityReference().getLink().getHref());
        }
        return null;
    }

    private boolean relationshipHasPositions(Relationship relationship) {
        return relationship.getDetails() != null && relationship.getDetails().getEmployment() != null
            && CollectionUtils.isNotEmpty(relationship.getDetails().getEmployment().getPositions());
    }

    private void addDeletedRelationshipPersons(Set<String> deletedPersonsIDs, List<AbstractSerializableRoot> deleted) {
        if (CollectionUtils.isNotEmpty(deletedPersonsIDs)) {
            personService.getDocuments(getSearchParamsByIds(deletedPersonsIDs)).getResults()
                .forEach(person -> {
                    if (person != null) {
                        deleted.add(person.getRoot());
                    }
                });
        }
    }

    private LegalEntity getLegalEntityFromOffice(String officeId) {
        Office office = heraClient.getEntityByID(officeId, Office.class);
        String legalEntityResource = null;
        if (office != null && office.getSummary().getInstitution().getLink() != null) {
            legalEntityResource = office.getSummary().getInstitution().getLink().getHref();
        }

        if (legalEntityResource != null) {
            Optional<LegalEntity> legalEntity = heraClient.getEntityByHref(legalEntityResource, LegalEntity.class);
            if (legalEntity.isPresent()) {
                return legalEntity.get();
            } else {
                throw new EntityNotFoundException();
            }
        }
        return null;
    }
}
