package com.rallycourt.court.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rallycourt.auth.entity.CourtOwnerStatus;
import com.rallycourt.auth.entity.Role;
import com.rallycourt.auth.entity.User;
import com.rallycourt.court.dto.CourtRequest;
import com.rallycourt.court.entity.Court;
import com.rallycourt.court.entity.CourtStatus;
import com.rallycourt.court.entity.CourtType;
import com.rallycourt.court.entity.VenueType;
import com.rallycourt.court.exception.CourtValidationException;
import com.rallycourt.court.geocoding.Coordinates;
import com.rallycourt.court.geocoding.GeocodingService;
import com.rallycourt.court.repository.CourtRepository;
import com.rallycourt.court.repository.CourtTypeRepository;
import com.rallycourt.court.repository.CourtTypeVenueTypeRepository;
import com.rallycourt.court.repository.VenueTypeRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourtServiceTest {

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private GeocodingService geocodingService;

    @Mock
    private CourtAccessService courtAccessService;

    @Mock
    private CourtTypeRepository courtTypeRepository;

    @Mock
    private VenueTypeRepository venueTypeRepository;

    @Mock
    private CourtTypeVenueTypeRepository courtTypeVenueTypeRepository;

    @InjectMocks
    private CourtServiceImpl courtService;

    private User adminUser;
    private CourtType basketball;
    private CourtType badminton;
    private CourtType pickleball;
    private CourtType futsal;
    private VenueType indoor;
    private VenueType outdoor;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setEmail("adminrallycourt@rallycourt.local");
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setMobileNumber("09170000000");
        adminUser.setRole(role);
        adminUser.setCourtOwnerStatus(CourtOwnerStatus.APPROVED);

        basketball = courtType("BASKETBALL");
        badminton = courtType("BADMINTON");
        pickleball = courtType("PICKLEBALL");
        futsal = courtType("FUTSAL");
        indoor = venueType("INDOOR");
        outdoor = venueType("OUTDOOR");
    }

    @Test
    void createCourtPersistsConfiguredOperatingHours() {
        CourtRequest request = new CourtRequest();
        request.setName("Center Court");
        request.setLocation("Makati City");
        request.setCourtType("BASKETBALL");
        request.setVenueType("INDOOR");
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(22, 0));

        stubCreateDependencies(basketball, indoor);

        Court savedCourt = courtService.createCourt(request);

        assertEquals(LocalTime.of(8, 0), savedCourt.getOpenTime());
        assertEquals(LocalTime.of(22, 0), savedCourt.getCloseTime());
        assertEquals(adminUser, savedCourt.getOwner());
        assertEquals("BASKETBALL", savedCourt.getCourtType().getCode());
        assertEquals("INDOOR", savedCourt.getVenueType().getCode());
        assertEquals(CourtStatus.AVAILABLE, savedCourt.getStatus());
    }

    @Test
    void createCourtRejectsInvalidOperatingHours() {
        CourtRequest request = new CourtRequest();
        request.setName("Center Court");
        request.setLocation("Makati City");
        request.setCourtType("BASKETBALL");
        request.setVenueType("INDOOR");
        request.setOpenTime(LocalTime.of(22, 0));
        request.setCloseTime(LocalTime.of(8, 0));

        when(courtAccessService.getCurrentUser()).thenReturn(adminUser);

        assertThrows(CourtValidationException.class, () -> courtService.createCourt(request));
    }

    @Test
    void updateCourtSkipsGeocodingWhenLocationIsUnchanged() {
        CourtRequest request = new CourtRequest();
        request.setName("Center Court Updated");
        request.setLocation("Makati City");
        request.setCourtType("PICKLEBALL");
        request.setVenueType("OUTDOOR");
        request.setOpenTime(LocalTime.of(9, 0));
        request.setCloseTime(LocalTime.of(21, 0));

        Court court = new Court();
        court.setId(100L);
        court.setName("Center Court");
        court.setLocation("Makati City");
        court.setLatitude(14.5547);
        court.setLongitude(121.0244);
        court.setCourtType(basketball);
        court.setVenueType(indoor);
        court.setOpenTime(LocalTime.of(8, 0));
        court.setCloseTime(LocalTime.of(22, 0));
        court.setOwner(adminUser);

        when(courtAccessService.getCurrentUser()).thenReturn(adminUser);
        when(courtRepository.findById(100L)).thenReturn(Optional.of(court));
        when(courtTypeRepository.findByCode("PICKLEBALL")).thenReturn(Optional.of(pickleball));
        when(venueTypeRepository.findByCode("OUTDOOR")).thenReturn(Optional.of(outdoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("PICKLEBALL", "OUTDOOR"))
                .thenReturn(true);
        when(courtRepository.save(any(Court.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Court updatedCourt = courtService.updateCourt(100L, request);

        assertEquals("Center Court Updated", updatedCourt.getName());
        assertEquals(LocalTime.of(9, 0), updatedCourt.getOpenTime());
        assertEquals(LocalTime.of(21, 0), updatedCourt.getCloseTime());
        assertEquals(14.5547, updatedCourt.getLatitude());
        assertEquals(121.0244, updatedCourt.getLongitude());
        assertEquals("PICKLEBALL", updatedCourt.getCourtType().getCode());
        assertEquals("OUTDOOR", updatedCourt.getVenueType().getCode());
    }

    @Test
    void validateBasketballIndoorIsValid() {
        CourtRequest request = requestFor("BASKETBALL", "INDOOR");
        stubCreateDependencies(basketball, indoor);

        Court savedCourt = courtService.createCourt(request);

        assertEquals("BASKETBALL", savedCourt.getCourtType().getCode());
        assertEquals("INDOOR", savedCourt.getVenueType().getCode());
    }

    @Test
    void validateBasketballOutdoorIsValid() {
        CourtRequest request = requestFor("BASKETBALL", "OUTDOOR");
        stubCreateDependencies(basketball, outdoor);

        Court savedCourt = courtService.createCourt(request);

        assertEquals("BASKETBALL", savedCourt.getCourtType().getCode());
        assertEquals("OUTDOOR", savedCourt.getVenueType().getCode());
    }

    @Test
    void validateBadmintonIndoorIsValid() {
        CourtRequest request = requestFor("BADMINTON", "INDOOR");
        stubCreateDependencies(badminton, indoor);

        Court savedCourt = courtService.createCourt(request);

        assertEquals("BADMINTON", savedCourt.getCourtType().getCode());
        assertEquals("INDOOR", savedCourt.getVenueType().getCode());
    }

    @Test
    void validateBadmintonOutdoorIsInvalidAndDoesNotGeocode() {
        CourtRequest request = requestFor("BADMINTON", "OUTDOOR");

        when(courtAccessService.getCurrentUser()).thenReturn(adminUser);
        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("OUTDOOR")).thenReturn(Optional.of(outdoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("BADMINTON", "OUTDOOR"))
                .thenReturn(false);

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtService.createCourt(request)
        );

        assertEquals("BADMINTON is not supported for OUTDOOR venues", exception.getMessage());
        verify(geocodingService, never()).geocode(any());
    }

    @Test
    void validatePickleballIndoorIsValid() {
        CourtRequest request = requestFor("PICKLEBALL", "INDOOR");
        stubCreateDependencies(pickleball, indoor);

        Court savedCourt = courtService.createCourt(request);

        assertEquals("PICKLEBALL", savedCourt.getCourtType().getCode());
        assertEquals("INDOOR", savedCourt.getVenueType().getCode());
    }

    @Test
    void validatePickleballOutdoorIsValid() {
        CourtRequest request = requestFor("PICKLEBALL", "OUTDOOR");
        stubCreateDependencies(pickleball, outdoor);

        Court savedCourt = courtService.createCourt(request);

        assertEquals("PICKLEBALL", savedCourt.getCourtType().getCode());
        assertEquals("OUTDOOR", savedCourt.getVenueType().getCode());
    }

    @Test
    void validateFutsalIndoorIsValid() {
        CourtRequest request = requestFor("FUTSAL", "INDOOR");
        stubCreateDependencies(futsal, indoor);

        Court savedCourt = courtService.createCourt(request);

        assertEquals("FUTSAL", savedCourt.getCourtType().getCode());
        assertEquals("INDOOR", savedCourt.getVenueType().getCode());
    }

    @Test
    void validateFutsalOutdoorIsInvalidAndDoesNotGeocode() {
        CourtRequest request = requestFor("FUTSAL", "OUTDOOR");

        when(courtAccessService.getCurrentUser()).thenReturn(adminUser);
        when(courtTypeRepository.findByCode("FUTSAL")).thenReturn(Optional.of(futsal));
        when(venueTypeRepository.findByCode("OUTDOOR")).thenReturn(Optional.of(outdoor));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code("FUTSAL", "OUTDOOR"))
                .thenReturn(false);

        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtService.createCourt(request)
        );

        assertEquals("FUTSAL is not supported for OUTDOOR venues", exception.getMessage());
        verify(geocodingService, never()).geocode(any());
    }

    @Test
    void getCourtsAppliesValidatedFilters() {
        Court court = new Court();
        court.setName("Center Court");

        when(courtTypeRepository.findByCode("BADMINTON")).thenReturn(Optional.of(badminton));
        when(venueTypeRepository.findByCode("INDOOR")).thenReturn(Optional.of(indoor));
        when(courtRepository.findAllFiltered("BADMINTON", "INDOOR", CourtStatus.AVAILABLE, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(court), PageRequest.of(0, 10), 1));

        var response = courtService.getCourts("badminton", "indoor", "available", 0, 10);

        assertEquals(1, response.totalElements());
        assertEquals("Center Court", response.content().get(0).getName());
    }

    @Test
    void getCourtsRejectsUnsupportedStatusFilter() {
        CourtValidationException exception = assertThrows(
                CourtValidationException.class,
                () -> courtService.getCourts(null, null, "maintenance", 0, 10)
        );

        assertEquals("Unsupported court status: maintenance", exception.getMessage());
    }

    private CourtRequest requestFor(String courtType, String venueType) {
        CourtRequest request = new CourtRequest();
        request.setName("Center Court");
        request.setLocation("Makati City");
        request.setCourtType(courtType);
        request.setVenueType(venueType);
        request.setOpenTime(LocalTime.of(8, 0));
        request.setCloseTime(LocalTime.of(22, 0));
        return request;
    }

    private void stubCreateDependencies(CourtType courtType, VenueType venueType) {
        when(courtAccessService.getCurrentUser()).thenReturn(adminUser);
        when(courtTypeRepository.findByCode(courtType.getCode())).thenReturn(Optional.of(courtType));
        when(venueTypeRepository.findByCode(venueType.getCode())).thenReturn(Optional.of(venueType));
        when(courtTypeVenueTypeRepository.existsByCourtType_CodeAndVenueType_Code(
                courtType.getCode(),
                venueType.getCode()
        )).thenReturn(true);
        when(geocodingService.geocode("Makati City")).thenReturn(new Coordinates(14.5547, 121.0244));
        when(courtRepository.save(any(Court.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private CourtType courtType(String code) {
        CourtType courtType = new CourtType();
        courtType.setCode(code);
        return courtType;
    }

    private VenueType venueType(String code) {
        VenueType venueType = new VenueType();
        venueType.setCode(code);
        return venueType;
    }
}
