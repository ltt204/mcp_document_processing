package individual.ltt204;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import individual.ltt204.entities.Job;
import individual.ltt204.services.job.JobService;
import individual.ltt204.services.parsing.IParsingService;
import individual.ltt204.services.storage.IStorageService;

@ExtendWith(MockitoExtension.class)
public class JobServiceTest {
    @Mock
    private IParsingService parsingService;
    @Mock
    private IStorageService<Job> jobStorageService;
    private ExecutorService pool;

    private JobService jobService;

    @BeforeEach
    public void setUp() {
        pool = Executors.newFixedThreadPool(2);
        jobService = new JobService(jobStorageService, pool);
    }

}
