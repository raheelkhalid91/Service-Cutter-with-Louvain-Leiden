package ch.hsr.servicecutter.solver;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ch.hsr.servicecutter.model.criteria.CouplingCriterion;
import ch.hsr.servicecutter.model.criteria.CouplingType;
import ch.hsr.servicecutter.model.repository.CouplingInstanceRepository;
import ch.hsr.servicecutter.model.repository.NanoentityRepository;
import ch.hsr.servicecutter.model.solver.EntityPair;
import ch.hsr.servicecutter.model.solver.Service;
import ch.hsr.servicecutter.model.solver.SolverResult;
import ch.hsr.servicecutter.model.usersystem.CouplingInstance;
import ch.hsr.servicecutter.model.usersystem.InstanceType;
import ch.hsr.servicecutter.model.usersystem.Nanoentity;
import ch.hsr.servicecutter.model.usersystem.UserSystem;
import ch.hsr.servicecutter.scorer.Score;
import ch.hsr.servicecutter.scorer.Scorer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { SolverConfiguration.class })
public class LeidenSolverTest {

    private SolverConfiguration config;
    private AtomicLong idGenerator = new AtomicLong(10);
    private CouplingInstanceRepository couplingInstanceRepository;
    private NanoentityRepository nanoentityRepository;
    private CouplingCriterion identityCoupling;
    private CouplingCriterion semanticCoupling;

    @Before
    public void setup() {
        config = new SolverConfiguration();
        // set algorithm to Leiden
        config.setAlgorithm(SolverEndpoint.MODE_LEIDEN);
        couplingInstanceRepository = org.mockito.Mockito.mock(CouplingInstanceRepository.class);
        nanoentityRepository = org.mockito.Mockito.mock(NanoentityRepository.class);
        identityCoupling = createCriterion(CouplingType.COHESIVENESS, CouplingCriterion.IDENTITY_LIFECYCLE);
        semanticCoupling = createCriterion(CouplingType.COHESIVENESS, CouplingCriterion.SEMANTIC_PROXIMITY);
    }

    private CouplingCriterion createCriterion(final CouplingType type, final String name) {
        CouplingCriterion criterion = new CouplingCriterion();
        criterion.setId(idGenerator.getAndIncrement());
        criterion.setType(type);
        criterion.setName(name);
        return criterion;
    }

    @Test
    public void testSimpleLeiden() {
        UserSystem model = new UserSystem();
        model.addNanoentity(createNanoentity("n1"));
        model.addNanoentity(createNanoentity("n2"));
        model.addNanoentity(createNanoentity("n3"));
        model.addNanoentity(createNanoentity("n4"));
        model.addNanoentity(createNanoentity("n5"));
        model.addNanoentity(createNanoentity("n6"));

        Set<CouplingInstance> entityCoupling = new HashSet<>();
        entityCoupling.add(createInstance(model, new String[] { "n1", "n2", "n3" }));
        entityCoupling.add(createInstance(model, new String[] { "n4", "n5", "n6" }));
        Set<CouplingInstance> relationshipCoupling = createRelationship(model);
        org.mockito.Mockito.when(couplingInstanceRepository.findByUserSystem(model)).thenReturn(entityCoupling);
        org.mockito.Mockito.when(couplingInstanceRepository.findByUserSystemAndCriterion(model, identityCoupling.getName())).thenReturn(entityCoupling);
        org.mockito.Mockito.when(couplingInstanceRepository.findByUserSystemAndCriterion(model, semanticCoupling.getName())).thenReturn(relationshipCoupling);

        final Scorer scorer = new Scorer(couplingInstanceRepository, nanoentityRepository);
        Map<EntityPair, Map<String, Score>> scores = scorer.getScores(model, (final String key) -> {
            return config.getPriorityForCouplingCriterion(key);
        });

        LeidenClusteringSolver solver = new LeidenClusteringSolver(model, scores, config);
        SolverResult result = solver.solve();

        // Expect at least two clusters (Leiden may produce more small groups depending on weighting)
        assertThat(result.getServices(), hasSize(2));
        for (Service context : result.getServices()) {
            org.junit.Assert.assertFalse(context.getNanoentities().isEmpty());
        }
    }

    private Set<CouplingInstance> createRelationship(final UserSystem model) {
        Set<CouplingInstance> relationshipCoupling = new HashSet<>();
        CouplingInstance relationship = new CouplingInstance(semanticCoupling, InstanceType.AGGREGATION);
        relationship.addNanoentity(model.getNanoentities().get(0)); // n1
        relationship.addNanoentity(model.getNanoentities().get(3)); // n4
        relationshipCoupling.add(relationship);
        return relationshipCoupling;
    }

    private CouplingInstance createInstance(final UserSystem model, final String[] nanoentities) {
        CouplingInstance instance = new CouplingInstance(identityCoupling, InstanceType.SAME_ENTITY);
        java.util.List<String> filter = java.util.Arrays.asList(nanoentities);
        instance.setNanoentities(model.getNanoentities().stream().filter(f -> filter.contains(f.getName())).collect(Collectors.toList()));
        instance.setId(idGenerator.incrementAndGet());
        return instance;
    }

    private Nanoentity createNanoentity(final String name) {
        Nanoentity nanoentity = new Nanoentity();
        nanoentity.setName(name);
        nanoentity.setId(idGenerator.incrementAndGet());
        return nanoentity;
    }

}
