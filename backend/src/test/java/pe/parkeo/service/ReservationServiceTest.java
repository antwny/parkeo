package pe.parkeo.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.parkeo.dto.request.CreateReservationRequest;
import pe.parkeo.dto.response.ReservationResponse;
import pe.parkeo.entity.*;
import pe.parkeo.enums.ReservationStatus;
import pe.parkeo.enums.SpaceStatus;
import pe.parkeo.exception.ConflictException;
import pe.parkeo.exception.ValidationException;
import pe.parkeo.repository.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReservationService Tests")
class ReservationServiceTest {

    @Mock ReservationRepository reservationRepository;
    @Mock ParkingSpaceRepository parkingSpaceRepository;
    @Mock VehicleRepository vehicleRepository;
    @Mock UserRepository userRepository;
    @Mock AvailabilityService availabilityService;

    @InjectMocks
    ReservationService reservationService;

    private User user;
    private ParkingLot parkingLot;
    private ParkingSpace parkingSpace;
    private Vehicle vehicle;
    private VehicleType vehicleType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @BeforeEach
    void setUp() {
        startTime = LocalDateTime.now().plusHours(1);
        endTime = LocalDateTime.now().plusHours(3);

        vehicleType = VehicleType.builder().id(1L).name("Auto").build();

        user = User.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .isActive(true)
                .build();

        parkingLot = ParkingLot.builder()
                .id(1L)
                .name("Parking Central")
                .address("Av. Principal 123")
                .latitude(-12.046374)
                .longitude(-77.042793)
                .totalSpaces(50)
                .availableSpaces(10)
                .isActive(true)
                .isOpen(true)
                .build();

        parkingSpace = ParkingSpace.builder()
                .id(1L)
                .parkingLot(parkingLot)
                .spaceNumber("A-01")
                .floor("1")
                .section("A")
                .status(SpaceStatus.AVAILABLE)
                .vehicleType(vehicleType)
                .build();

        vehicle = Vehicle.builder()
                .id(1L)
                .user(user)
                .vehicleType(vehicleType)
                .licensePlate("ABC-123")
                .brand("Toyota")
                .model("Corolla")
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("createReservation: debe crear reserva exitosamente")
    void createReservation_shouldSucceed() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        Reservation savedReservation = Reservation.builder()
                .id(1L)
                .user(user)
                .parkingSpace(parkingSpace)
                .vehicle(vehicle)
                .parkingLot(parkingLot)
                .startTime(startTime)
                .endTime(endTime)
                .status(ReservationStatus.PENDING)
                .totalAmount(new BigDecimal("20.00"))
                .currency("PEN")
                .confirmationCode("PKO-ABC12345")
                .build();

        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(parkingSpace));
        when(vehicleRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.findOverlappingForUpdate(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(availabilityService.calculatePrice(anyLong(), anyLong(), any(), any()))
                .thenReturn(new BigDecimal("20.00"));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(savedReservation);

        ReservationResponse response = reservationService.createReservation(request, 1L);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("20.00");
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    @DisplayName("createReservation: debe lanzar ConflictException cuando espacio ya está reservado")
    void createReservation_shouldThrowConflict_whenSpaceOverlaps() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        Reservation existingReservation = Reservation.builder()
                .id(99L)
                .parkingSpace(parkingSpace)
                .startTime(startTime.minusMinutes(30))
                .endTime(endTime.plusMinutes(30))
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(parkingSpace));
        when(vehicleRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(reservationRepository.findOverlappingForUpdate(anyLong(), any(), any()))
                .thenReturn(List.of(existingReservation));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("reservado");

        verify(reservationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createReservation: debe lanzar ValidationException cuando endTime <= startTime")
    void createReservation_shouldThrowValidation_whenInvalidTimeRange() {
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(startTime.minusMinutes(30)) // end before start
                .build();

        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(parkingSpace));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("createReservation: debe lanzar ValidationException cuando espacio está en mantenimiento")
    void createReservation_shouldThrowValidation_whenSpaceInMaintenance() {
        parkingSpace.setStatus(SpaceStatus.MAINTENANCE);
        CreateReservationRequest request = CreateReservationRequest.builder()
                .parkingSpaceId(1L)
                .vehicleId(1L)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        when(parkingSpaceRepository.findById(1L)).thenReturn(Optional.of(parkingSpace));

        assertThatThrownBy(() -> reservationService.createReservation(request, 1L))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("MAINTENANCE");
    }
}
