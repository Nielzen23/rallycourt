package com.rallycourt.court;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rallycourt.AbstractIntegrationTest;
import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.auth.repository.RoleRepository;
import com.rallycourt.auth.repository.UserRepository;
import com.rallycourt.auth.service.JwtService;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.entity.CourtStatus;
import com.rallycourt.court.entity.CourtType;
import com.rallycourt.court.entity.VenueType;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.court.repository.CourtTypeRepository;
import com.rallycourt.court.repository.VenueTypeRepository;
import com.rallycourt.payment.repository.PaymentRepository;
import com.rallycourt.reservation.repository.ReservationRepository;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

class CourtIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourtRepository courtRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CourtTypeRepository courtTypeRepository;

    @Autowired
    private VenueTypeRepository venueTypeRepository;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    private String adminToken;
    private String ownerToken;
    private String playerToken;
    private User adminUser;
    private User ownerUser;
    private User playerUser;
    private static final String ADMIN_EMAIL = "adminrallycourt@rallycourt.local";

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reservationRepository.deleteAll();
        courtRepository.deleteAll();
        adminUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        ownerUser = ensureUser("courtownerone@rallycourt.local", "Court", "Owner", "COURT_OWNER", CourtOwnerStatus.APPROVED);
        playerUser = ensureUser("playerone@rallycourt.local", "Player", "One", "PLAYER", CourtOwnerStatus.NONE);
        adminToken = jwtService.generateToken(adminUser);
        ownerToken = jwtService.generateToken(ownerUser);
        playerToken = jwtService.generateToken(playerUser);
    }

    @Test
    void getCourtsReturnsStoredCoordinates() throws Exception {
        Court firstCourt = new Court();
        firstCourt.setName("Center Court");
        firstCourt.setLocation("Makati City");
        firstCourt.setLatitude(14.5547);
        firstCourt.setLongitude(121.0244);
        firstCourt.setCourtType(courtType("BASKETBALL"));
        firstCourt.setVenueType(venueType("INDOOR"));
        firstCourt.setStatus(CourtStatus.AVAILABLE);
        firstCourt.setOpenTime(LocalTime.of(8, 0));
        firstCourt.setCloseTime(LocalTime.of(22, 0));
        firstCourt.setOwner(adminUser);
        courtRepository.save(firstCourt);

        Court secondCourt = new Court();
        secondCourt.setName("North Court");
        secondCourt.setLocation("Quezon City");
        secondCourt.setLatitude(14.6760);
        secondCourt.setLongitude(121.0437);
        secondCourt.setCourtType(courtType("PICKLEBALL"));
        secondCourt.setVenueType(venueType("OUTDOOR"));
        secondCourt.setStatus(CourtStatus.UNAVAILABLE);
        secondCourt.setOpenTime(LocalTime.of(8, 0));
        secondCourt.setCloseTime(LocalTime.of(22, 0));
        secondCourt.setOwner(adminUser);
        courtRepository.save(secondCourt);

        mockMvc.perform(get("/api/courts")
                        .param("page", "0")
                        .param("size", "1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Center Court"))
                .andExpect(jsonPath("$.content[0].latitude").value(14.5547))
                .andExpect(jsonPath("$.content[0].longitude").value(121.0244))
                .andExpect(jsonPath("$.content[0].courtType").value("BASKETBALL"))
                .andExpect(jsonPath("$.content[0].venueType").value("INDOOR"))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void getCourtsSupportsFilteringByCourtTypeVenueTypeAndStatus() throws Exception {
        Court firstCourt = new Court();
        firstCourt.setName("Center Court");
        firstCourt.setLocation("Makati City");
        firstCourt.setLatitude(14.5547);
        firstCourt.setLongitude(121.0244);
        firstCourt.setCourtType(courtType("BADMINTON"));
        firstCourt.setVenueType(venueType("INDOOR"));
        firstCourt.setStatus(CourtStatus.AVAILABLE);
        firstCourt.setOpenTime(LocalTime.of(8, 0));
        firstCourt.setCloseTime(LocalTime.of(22, 0));
        firstCourt.setOwner(adminUser);
        courtRepository.save(firstCourt);

        Court secondCourt = new Court();
        secondCourt.setName("North Court");
        secondCourt.setLocation("Quezon City");
        secondCourt.setLatitude(14.6760);
        secondCourt.setLongitude(121.0437);
        secondCourt.setCourtType(courtType("BADMINTON"));
        secondCourt.setVenueType(venueType("OUTDOOR"));
        secondCourt.setStatus(CourtStatus.AVAILABLE);
        secondCourt.setOpenTime(LocalTime.of(8, 0));
        secondCourt.setCloseTime(LocalTime.of(22, 0));
        secondCourt.setOwner(adminUser);
        courtRepository.save(secondCourt);

        Court thirdCourt = new Court();
        thirdCourt.setName("South Court");
        thirdCourt.setLocation("Pasig City");
        thirdCourt.setLatitude(14.5764);
        thirdCourt.setLongitude(121.0851);
        thirdCourt.setCourtType(courtType("PICKLEBALL"));
        thirdCourt.setVenueType(venueType("INDOOR"));
        thirdCourt.setStatus(CourtStatus.UNAVAILABLE);
        thirdCourt.setOpenTime(LocalTime.of(8, 0));
        thirdCourt.setCloseTime(LocalTime.of(22, 0));
        thirdCourt.setOwner(adminUser);
        courtRepository.save(thirdCourt);

        mockMvc.perform(get("/api/courts")
                        .param("courtType", "BADMINTON")
                        .param("venueType", "INDOOR")
                        .param("status", "AVAILABLE")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Center Court"));
    }

    @Test
    void getCourtsRejectsInvalidFilterValues() throws Exception {
        mockMvc.perform(get("/api/courts")
                        .param("courtType", "TENNIS")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/courts")
                        .param("venueType", "ROOFED")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/courts")
                        .param("status", "MAINTENANCE")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCourtsRejectsInvalidPaginationLimits() throws Exception {
        mockMvc.perform(get("/api/courts")
                        .param("page", "-1")
                        .param("size", "51")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }

    @Test
    void courtManagementEndpointsRejectNonManagerUsers() throws Exception {
        mockMvc.perform(post("/api/court-management/courts")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rally Court QC",
                                  "location": "Quezon City",
                                  "courtType": "BADMINTON",
                                  "venueType": "INDOOR",
                                  "status": "AVAILABLE",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndViewCourtManagementDetails() throws Exception {
        mockMvc.perform(post("/api/court-management/courts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rally Court QC",
                                  "location": "Quezon City",
                                  "courtType": "BADMINTON",
                                  "venueType": "INDOOR",
                                  "status": "AVAILABLE",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rally Court QC"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"));

        Court createdCourt = courtRepository.findAll().stream()
                .filter(court -> "Rally Court QC".equals(court.getName()))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/court-management/courts/{courtId}", createdCourt.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rally Court QC"))
                .andExpect(jsonPath("$.courtType").value("BADMINTON"))
                .andExpect(jsonPath("$.upcomingReservations").isArray());
    }

    @Test
    void courtOwnerCanAccessCourtManagementEndpoints() throws Exception {
        mockMvc.perform(post("/api/court-management/courts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Owner Managed Court",
                                  "location": "Makati City",
                                  "courtType": "BADMINTON",
                                  "venueType": "INDOOR",
                                  "status": "AVAILABLE",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Owner Managed Court"));
    }

    @Test
    void createCourtGeocodesAndPersistsCoordinates() throws Exception {
        mockMvc.perform(post("/api/courts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rally Court BGC",
                                  "location": "Bonifacio Global City",
                                  "courtType": "BASKETBALL",
                                  "venueType": "OUTDOOR",
                                  "openTime": "08:00:00",
                                  "closeTime": "22:00:00",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Rally Court BGC"))
                .andExpect(jsonPath("$.owner.email").value(ADMIN_EMAIL))
                .andExpect(jsonPath("$.openTime").value("08:00:00"))
                .andExpect(jsonPath("$.closeTime").value("22:00:00"))
                .andExpect(jsonPath("$.courtType").value("BASKETBALL"))
                .andExpect(jsonPath("$.venueType").value("OUTDOOR"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.latitude").value(14.5507))
                .andExpect(jsonPath("$.longitude").value(121.0505));
    }

    @Test
    void createCourtRejectsNonAdminAndNonCourtOwner() throws Exception {
        mockMvc.perform(post("/api/courts")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Player Court",
                                  "location": "Pasig City",
                                  "courtType": "BASKETBALL",
                                  "venueType": "INDOOR",
                                  "openTime": "08:00:00",
                                  "closeTime": "22:00:00",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCourtRejectsInvalidOperatingHours() throws Exception {
        mockMvc.perform(post("/api/courts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Invalid Hours Court",
                                  "location": "Makati City",
                                  "courtType": "BASKETBALL",
                                  "venueType": "INDOOR",
                                  "openTime": "22:00:00",
                                  "closeTime": "08:00:00"
                                }
                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCourtRejectsInvalidCourtAndVenueCombination() throws Exception {
        mockMvc.perform(post("/api/courts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Outdoor Futsal Court",
                                  "location": "Makati City",
                                  "courtType": "FUTSAL",
                                  "venueType": "OUTDOOR",
                                  "openTime": "08:00:00",
                                  "closeTime": "22:00:00",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("FUTSAL is not supported for OUTDOOR venues"));
    }

    @Test
    void updateCourtGeocodesWhenLocationChanges() throws Exception {
        Court court = new Court();
        court.setName("Rally Court BGC");
        court.setLocation("Bonifacio Global City");
        court.setLatitude(14.5507);
        court.setLongitude(121.0505);
        court.setCourtType(courtType("BASKETBALL"));
        court.setVenueType(venueType("OUTDOOR"));
        court.setStatus(CourtStatus.AVAILABLE);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setOwner(ownerUser);
        court = courtRepository.save(court);

        mockMvc.perform(put("/api/courts/{id}", court.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rally Court Ortigas",
                                  "location": "Ortigas Center",
                                  "courtType": "PICKLEBALL",
                                  "venueType": "INDOOR",
                                  "openTime": "09:00:00",
                                  "closeTime": "21:00:00",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rally Court Ortigas"))
                .andExpect(jsonPath("$.openTime").value("09:00:00"))
                .andExpect(jsonPath("$.closeTime").value("21:00:00"))
                .andExpect(jsonPath("$.courtType").value("PICKLEBALL"))
                .andExpect(jsonPath("$.venueType").value("INDOOR"))
                .andExpect(jsonPath("$.latitude").value(14.5869))
                .andExpect(jsonPath("$.longitude").value(121.0614));
    }

    @Test
    void updateCourtSkipsGeocodingWhenLocationIsUnchanged() throws Exception {
        Court court = new Court();
        court.setName("Rally Court BGC");
        court.setLocation("Bonifacio Global City");
        court.setLatitude(14.5507);
        court.setLongitude(121.0505);
        court.setCourtType(courtType("BASKETBALL"));
        court.setVenueType(venueType("INDOOR"));
        court.setStatus(CourtStatus.AVAILABLE);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setOwner(ownerUser);
        court = courtRepository.save(court);

        mockMvc.perform(put("/api/courts/{id}", court.getId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Rally Court BGC Updated",
                                  "location": "Bonifacio Global City",
                                  "courtType": "BADMINTON",
                                  "venueType": "INDOOR",
                                  "openTime": "08:30:00",
                                  "closeTime": "21:30:00",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Rally Court BGC Updated"))
                .andExpect(jsonPath("$.openTime").value("08:30:00"))
                .andExpect(jsonPath("$.closeTime").value("21:30:00"))
                .andExpect(jsonPath("$.courtType").value("BADMINTON"))
                .andExpect(jsonPath("$.venueType").value("INDOOR"))
                .andExpect(jsonPath("$.latitude").value(14.5507))
                .andExpect(jsonPath("$.longitude").value(121.0505));
    }

    @Test
    void updateCourtAllowsAdminOverride() throws Exception {
        Court court = new Court();
        court.setName("Owner Court");
        court.setLocation("Quezon City");
        court.setLatitude(14.6760);
        court.setLongitude(121.0437);
        court.setCourtType(courtType("BASKETBALL"));
        court.setVenueType(venueType("OUTDOOR"));
        court.setStatus(CourtStatus.AVAILABLE);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setOwner(ownerUser);
        court = courtRepository.save(court);

        mockMvc.perform(put("/api/courts/{id}", court.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {
                                  "name": "Admin Attempt",
                                  "location": "Quezon City",
                                  "courtType": "BASKETBALL",
                                  "venueType": "INDOOR",
                                  "openTime": "08:00:00",
                                  "closeTime": "22:00:00",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Admin Attempt"));
    }

    @Test
    void updateCourtRejectsDifferentNonAdminUser() throws Exception {
        Court court = new Court();
        court.setName("Owner Court");
        court.setLocation("Quezon City");
        court.setLatitude(14.6760);
        court.setLongitude(121.0437);
        court.setCourtType(courtType("BASKETBALL"));
        court.setVenueType(venueType("OUTDOOR"));
        court.setStatus(CourtStatus.AVAILABLE);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setOwner(ownerUser);
        court = courtRepository.save(court);

        mockMvc.perform(put("/api/courts/{id}", court.getId())
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Player Attempt",
                                  "location": "Quezon City",
                                  "courtType": "BASKETBALL",
                                  "venueType": "INDOOR",
                                  "openTime": "08:00:00",
                                  "closeTime": "22:00:00",
                                  "hourlyRate": 750
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private User ensureUser(String email, String firstName, String lastName, String roleCode, CourtOwnerStatus courtOwnerStatus) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> {
                    Role role = roleRepository.findByCode(roleCode).orElseThrow();
                    User user = new User();
                    user.setEmail(email);
                    user.setFirstName(firstName);
                    user.setLastName(lastName);
                    user.setMobileNumber("09170000000");
                    user.setPassword(passwordEncoder.encode("RallyCourt123"));
                    user.setRole(role);
                    user.setCourtOwnerStatus(courtOwnerStatus);
                    return userRepository.save(user);
                });
    }

    private CourtType courtType(String code) {
        return courtTypeRepository.findByCode(code).orElseThrow();
    }

    private VenueType venueType(String code) {
        return venueTypeRepository.findByCode(code).orElseThrow();
    }
}
