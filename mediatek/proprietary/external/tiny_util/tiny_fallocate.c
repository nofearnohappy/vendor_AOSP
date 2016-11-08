#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/syscall.h>
#include <errno.h>

#define CURRENT_PROGNAME	"tiny_fallocate"

static void __attribute__ ((__noreturn__)) usage(FILE *out)
{
	fprintf(out,"\nUsage:\n"
		    " %s [options] length filename\n",CURRENT_PROGNAME);

	fprintf(out,"\nOptions:\n"
		    " -h, display this help and exit\n\n");

	exit(out == stderr ? EXIT_FAILURE : EXIT_SUCCESS);
}

int main(int argc, char **argv)
{
	int 	c;
	int	fd;
	loff_t	length = -2LL;
	char 	*filename;
	int 	error;

	/* Get options */
	while((c = getopt(argc, argv, "h")) != -1) {
		switch(c) {
		case 'h':
		default :
			usage(stdout);
		}
	}

	/* This tiny utility for fallocate only needs 2 parameters: length & filename */
	if (argc != 3)
		usage(stderr);

	/* Get length */
	length = strtoll(argv[optind++], NULL, 0);
	fprintf(stdout, "Length: %lld %llu\n", length, length);
	if (length <= 0)
		usage(stderr);

	/* Get filename */
	filename = argv[optind++];

	/* Needless check... */
	if (optind != argc)
		usage(stderr);

	/* Create it if the file does not exist */
	fd = open(filename, O_RDWR | O_CREAT, 0644);
	if (fd < 0) {
		fprintf(stderr, "Failed to open/create %s\n", filename);
		exit(EXIT_FAILURE);
	}

	/* Call fallocate */
	error = fallocate(fd, 0, 0, length);
	if (error < 0) {
		fprintf(stderr, "fallocate failed [%s]\n", strerror(errno));
		exit(EXIT_FAILURE);
	}

	/* Close */
	if (close(fd) != 0) {
		fprintf(stderr, "Failed to close %s\n", filename);
		exit(EXIT_FAILURE);
	}

	return EXIT_SUCCESS;
}
